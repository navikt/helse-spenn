package no.nav.helse.spenn.avstemming

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import java.util.*

internal class KafkaConfig(
    private val bootstrapServers: String,
    private val truststore: String? = null,
    private val truststorePassword: String? = null,
    private val keystoreLocation: String? = null,
    private val keystorePassword: String? = null,
) {

    internal fun producerConfig() = Properties().apply {
        putAll(kafkaBaseConfig())
        put(ProducerConfig.ACKS_CONFIG, "1")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststore)
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation)
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword)
    }
}
