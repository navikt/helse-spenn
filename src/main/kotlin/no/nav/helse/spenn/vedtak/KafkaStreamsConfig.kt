package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.Environment
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.metrics.VEDTAK
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppslag.AktørTilFnrMapper
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.springframework.dao.DuplicateKeyException
import java.io.File
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct


@Configuration
@AutoConfigureAfter(FlywayAutoConfiguration::class)
class KafkaStreamsConfig(val oppdragStateService: OppdragStateService,
                         val meterRegistry: MeterRegistry,
                         val aktørTilFnrMapper: AktørTilFnrMapper,
                         val env: Environment,
                         @Value("\${kafka.offset-reset.timestamp-millis}") val timeStampMillis: Long,
                         @Value("\${kafka.offset-reset.enabled}") val offsetReset: String) {

    companion object {
        private val log = LoggerFactory.getLogger(KafkaStreamsConfig::class.java)
    }

    @PostConstruct
    fun offsetReset() {
        if ("true" == offsetReset) {
            log.info("Running offset reset of ${VEDTAK_SYKEPENGER.name} to ${timeStampMillis}")
            val configProperties = Properties()
            configProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
            configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            configProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            configProperties.put(ConsumerConfig.GROUP_ID_CONFIG, env.appId)
            configProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false)
            configProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")
            if ("false" == env.plainTextKafka) {
                configProperties.put(SaslConfigs.SASL_MECHANISM, "PLAIN")
                configProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                configProperties.put(SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.kafkaUsername}\" password=\"${env.kafkaPassword}\";")
                configProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                configProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(env.navTruststorePath).absolutePath)
                configProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.navTruststorePassword)
            }
            val offsetConsumer = KafkaConsumer<String, String>(configProperties)
            offsetConsumer.subscribe(listOf(VEDTAK_SYKEPENGER.name),  object: ConsumerRebalanceListener {
                override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>) {
                    for (p in partitions) {
                        log.info("assigned to ${p.topic()} to partion: ${p.partition()}")
                        val offsetsForTimes = offsetConsumer.offsetsForTimes(mapOf(p to timeStampMillis))
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

    @Bean
    fun kafkaStreams(topology: Topology) : KafkaStreams {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(env.appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        return KafkaStreams(topology, streamConfig)
    }

    @Bean
    fun kafkaStreamTopology() : Topology {
        val builder = StreamsBuilder()

        builder.consumeTopic(VEDTAK_SYKEPENGER)
                .peek{ key: String, _ ->
                    log.info("soknad id ${key}")
                }
                .mapValues { key: String, node: JsonNode -> node.tilVedtak(key) }
                .mapValues { _, vedtak -> Pair<Fodselsnummer, Vedtak>(aktørTilFnrMapper.tilFnr(vedtak.aktørId),vedtak) }
                .mapValues { _,  (fodselnummer, vedtak) -> vedtak.tilUtbetaling(fodselnummer) }
                .mapValues { key: String, utbetaling -> saveInitialOppdragState(key, utbetaling) }
                .filter { _, value ->  value != null}
                .peek {_,_ -> meterRegistry.counter(VEDTAK).increment() }

        return builder.build()
    }

    private fun saveInitialOppdragState(key: String, utbetaling: UtbetalingsOppdrag): OppdragStateDTO? {
        return try { oppdragStateService.saveOppdragState(
                OppdragStateDTO(soknadId = UUID.fromString(key),
                        utbetalingsOppdrag = utbetaling))
        }
        catch (e: DuplicateKeyException) {
            log.warn("skipping duplicate for key ${key}")
            return null
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

    @Bean
    fun streamConsumer(kafkaStreams: KafkaStreams, flywayMigrationInitializer: FlywayMigrationInitializer?) : StreamConsumer {
        if (flywayMigrationInitializer == null) throw ExceptionInInitializerError("Kafka needs flyway migration to finished")
        val streamConsumer = StreamConsumer(env.appId, kafkaStreams)
        streamConsumer.start()
        return streamConsumer
    }


    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, env.appId)
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


