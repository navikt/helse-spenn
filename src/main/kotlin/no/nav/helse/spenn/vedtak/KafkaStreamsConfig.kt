package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.Environment
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.metrics.VEDTAK
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.*

@Configuration
class KafkaStreamsConfig(val utbetalingService: UtbetalingService,
                         val oppdragStateService: OppdragStateService,
                         val meterRegistry: MeterRegistry,
                         val aktørTilFnrMapper: AktørTilFnrMapper) {

    private val log = LoggerFactory.getLogger(KafkaStreamsConfig::class.java)

    @Bean
    fun kafkaStreams(env : Environment, topology: Topology) : KafkaStreams {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka(env) else streamConfig(env.appId, env.bootstrapServersUrl,
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
                    meterRegistry.counter(VEDTAK).increment()
                }
                .mapValues { key: String, node: JsonNode -> node.tilVedtak(key) }
                .mapValues { _, vedtak -> vedtak.tilUtbetaling(aktørTilFnrMapper) }
                .mapValues { key: String, utbetaling -> oppdragStateService.saveOppdragState(
                                            OppdragStateDTO(soknadId = UUID.fromString(key), utbetalingsOppdrag = utbetaling ))}
                .mapValues { _, oppdrag -> utbetalingService.runSimulering(oppdrag)}

        return builder.build()
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
    fun streamConsumer(env: Environment, kafkaStreams: KafkaStreams) : StreamConsumer {
        val consumer = StreamConsumer(env.appId, kafkaStreams)
        //consumer.start()
        return consumer
    }


    private fun streamConfigPlainTextKafka(env: Environment): Properties = Properties().apply {
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

