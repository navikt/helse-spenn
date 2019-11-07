package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.VEDTAK
import no.nav.helse.spenn.config.SpennKafkaConfig
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct

class KafkaStreamsConfig(val oppdragStateService: OppdragStateService,
                         val meterRegistry: MeterRegistry,
                         val aktørTilFnrMapper: AktørTilFnrMapper,
                         val config: SpennKafkaConfig) {

    companion object {
        private val log = LoggerFactory.getLogger(KafkaStreamsConfig::class.java)
    }

    fun mottakslogikk() : Topology {
        val builder = StreamsBuilder()

        builder.consumeTopic(SYKEPENGER_BEHOV_TOPIC)
                .filter { _, value -> value["@behov"]?.textValue() == "Utbetaling" }
                .filter { _, value -> value["@løsning"].let {
                    it == null || it.isNull
                }}
                .mapValues { _, value -> defaultObjectMapper.treeToValue<Utbetalingsbehov>(value) }
                .mapValues { _, value -> aktørTilFnrMapper.tilFnr(value.aktørId) to value }
                .mapValues { _,  (fodselnummer, value) -> value.tilUtbetaling(fodselnummer) }
                .mapValues { key: String, utbetaling -> saveInitialOppdragState(key, utbetaling) }
                .filter { _, value ->  value != null}
                .peek {_,_ -> meterRegistry.counter(VEDTAK, "status", "OK").increment() }

        return builder.build()
    }

    @PostConstruct
    fun offsetReset() {
        if (config.offsetReset) {
            log.info("Running offset reset of ${SYKEPENGER_BEHOV_TOPIC.name} to ${config.timeStampMillis}")
            val configProperties = Properties()
            configProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServersUrl)
            configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            configProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            configProperties.put(ConsumerConfig.GROUP_ID_CONFIG, config.appId)
            configProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false)
            configProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")
            if (!config.plainTextKafka) {
                configProperties.put(SaslConfigs.SASL_MECHANISM, "PLAIN")
                configProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                configProperties.put(SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${config.kafkaUsername}\" password=\"${config.kafkaPassword}\";")
                configProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                configProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(config.navTruststorePath).absolutePath)
                configProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config.navTruststorePassword)
            }
            val offsetConsumer = KafkaConsumer<String, String>(configProperties)
            offsetConsumer.subscribe(listOf(SYKEPENGER_BEHOV_TOPIC.name),  object: ConsumerRebalanceListener {
                override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>) {
                    for (p in partitions) {
                        log.info("assigned to ${p.topic()} to partion: ${p.partition()}")
                        val offsetsForTimes = offsetConsumer.offsetsForTimes(mapOf(p to config.timeStampMillis))
                        log.info("offset for times ${offsetsForTimes.entries}")
                        offsetsForTimes.forEach { topicPartition, offsetAndTimestamp ->
                            if (offsetAndTimestamp != null) {
                                log.info("reset offset to ${offsetAndTimestamp.offset()}")
                                offsetConsumer.seek(topicPartition, offsetAndTimestamp.offset())
                                offsetConsumer.commitSync()
                            }
                        }

                    }
                }

                override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>) {
                    log.info("partitions ${partitions.toString()} revoked")
                }
            })
            try {
                val records = offsetConsumer.poll(Duration.ofSeconds(5))
                for (record in records) {
                    log.info("offset ${record.offset()}")
                }
            } catch (ex: WakeupException) {
                log.error("Exception caught " + ex.message)
            } finally {
                offsetConsumer.close()
                log.info("Closing OffsetConsumer")
            }
        }
    }

    fun kafkaStreams(topology: Topology) : KafkaStreams {
        val streamConfig = if (config.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(config.appId, config.bootstrapServersUrl,
                config.kafkaUsername to config.kafkaPassword,
                config.navTruststorePath to config.navTruststorePassword)
        return KafkaStreams(topology, streamConfig)
    }

    private fun saveInitialOppdragState(key: String, utbetaling: UtbetalingsOppdrag): OppdragStateDTO? {
        return try { oppdragStateService.saveOppdragState(
                OppdragStateDTO(soknadId = UUID.fromString(key),
                        utbetalingsOppdrag = utbetaling))
        }
        catch (e: DataAccessException/*DuplicateKeyException*/) {
            if (e.cause is SQLIntegrityConstraintViolationException) {
                log.warn("skipping duplicate for key ${key}")
                meterRegistry.counter(VEDTAK, "status", "DUPLIKAT").increment()
                return null
            }
            throw e
        }
    }


    fun <K : Any, V : Any> StreamsBuilder.consumeTopic(topic: Topic<K, V>): KStream<K, V> {
        return consumeTopic(topic, null)
    }

    fun <K: Any, V: Any> StreamsBuilder.consumeTopic(topic: Topic<K, V>, schemaRegistryUrl: String?): KStream<K, V> {
        schemaRegistryUrl?.let {
            topic.keySerde.configure(mapOf(
                    AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), true)

            topic.valueSerde.configure(mapOf(
                    AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), false)
        }

        return stream<K, V>(
                topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
        )
    }

    fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
        return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
    }

    fun streamConsumerStart() : StreamConsumer{
        return streamConsumer(kafkaStreams(mottakslogikk()))
    }

    fun streamConsumer(kafkaStreams: KafkaStreams) : StreamConsumer {
        val streamConsumer = StreamConsumer(config.appId, kafkaStreams)
        if (config.streamVedtak) streamConsumer.start()
        return streamConsumer
    }


    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, config.appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    private fun streamConfig(
            appId: String,
            bootstrapServers: String,
            credentials: Pair<String?, String?>,
            truststore: Pair<String?, String?>
    ): Properties = Properties().apply {
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)

        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)

        credentials.first?.let {
            log.info("Using user name ${it} to authenticate against Kafka brokers ")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${it}\" password=\"${credentials.second}\";")
        }

        truststore.first?.let {
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststore.second)
                log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
                log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
        }
    }
}

data class Topic<K, V>(
        val name: String,
        val keySerde: Serde<K>,
        val valueSerde: Serde<V>
)
