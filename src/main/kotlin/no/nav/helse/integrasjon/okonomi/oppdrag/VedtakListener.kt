package no.nav.helse.integrasjon.okonomi.oppdrag

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import no.nav.helse.spenn.StreamConsumer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


private val strings = Serdes.String()
private val json = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
val VEDTAK_SYKEPENGER = Topic(
        name = "aapen-helse-sykepenger-vedtak",
        keySerde = strings,
        valueSerde = json
)

class VedtakListener(val env: Environment) {
    private val log = LoggerFactory.getLogger(VedtakListener::class.java)
    private val consumer: StreamConsumer
    private val appId: String = "spenn-1"
    init {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()

        builder.consumeTopic(VEDTAK_SYKEPENGER)
                .peek{_, _ -> log.info("here goes payments")}

        return builder.build()
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun streamConfig(
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

}

data class Topic<K, V>(
        val name: String,
        val keySerde: Serde<K>,
        val valueSerde: Serde<V>
)
