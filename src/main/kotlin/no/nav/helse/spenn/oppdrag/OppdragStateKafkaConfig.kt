package no.nav.helse.spenn.oppdrag

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.io.File

@Configuration
class OppdragStateKafkaConfig(@Value("\${KAFKA_BOOTSTRAP_SERVERS}") val bootstrapServersUrl: String,
                              @Value("\${KAFKA_APP_ID:spenn-1}") val appId: String,
                              @Value("\${KAFKA_USERNAME}") val kafkaUsername: String,
                              @Value("\${KAFKA_PASSWORD}") val kafkaPassword: String,
                              @Value("\${NAV_TRUSTSTORE_PATH}") val navTruststorePath: String,
                              @Value("\${NAV_TRUSTSTORE_PASSWORD}") val navTruststorePassword: String,
                              @Value("\${PLAIN_TEXT_KAFKA:false}") val plainTextKafka: String) {

    @Bean
    fun producerConfigs() : Map<String, Any> =
        HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersUrl)
            if ("true" != plainTextKafka) {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(navTruststorePath).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, navTruststorePassword)
            } else {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            }
            put(SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$kafkaUsername\" password=\"$kafkaPassword\";")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(ProducerConfig.CLIENT_ID_CONFIG, appId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        }

    @Bean
    fun producerFactory() : ProducerFactory<String, String> =
        DefaultKafkaProducerFactory(producerConfigs())

    @Bean
    fun kafkaTemplate() : KafkaTemplate<String, String> =
        KafkaTemplate(producerFactory())

}