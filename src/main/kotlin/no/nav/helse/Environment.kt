package no.nav.helse

import org.springframework.beans.factory.annotation.Value

class Environment {

    val appId = "spenn-1"

    @Value("\${KAFKA_USERNAME}")
    val kafkaUsername: String = ""

    @Value("\${KAFKA_PASSWORD}")
    val kafkaPassword: String= ""

    @Value("\${STS_SOAP_USERNAME}")
    val stsUsername: String= ""

    @Value("\${STS_SOAP_PASSWORD}")
    val stsPassword: String= ""

    @Value("\${SECURITYTOKENSERVICE_URL}")
    val stsUrl: String= ""

    @Value("\${KAFKA_BOOTSTRAP_SERVERS}")
    val bootstrapServersUrl: String= ""

    @Value("\${NAV_TRUSTSTORE_PATH}")
    val navTruststorePath: String= ""

    @Value("\${SPARKEL_BASE_URL:http://sparkel}")
    val sparkelBaseUrl: String= ""

    @Value("\${SECURITY_TOKEN_SERVICE_REST_URL}")
    val stsRestUrl: String= ""

    @Value("\${STS_REST_USERNAME}")
    val stsRestUsername: String= ""

    @Value("\${STS_REST_PASSWORD}")
    val stsRestPassword: String= ""

    @Value("\${PLAIN_TEXT_KAFKA}")
    val plainTextKafka: String= ""

    @Value("\${NAV_TRUSTSTORE_PASSWORD}")
    val navTruststorePassword: String= ""

    @Value("\${SIMULERING_SERVICE_URL}")
    val simuleringServiceUrl: String= ""

    @Value("\${MQ_HOSTNAME}")
    val mqHostname: String= ""

    @Value("\${MQ_CHANNEL}")
    val mqChannel: String= ""

    @Value("\${MQ_PORT}")
    val mqPort: String= ""

    @Value("\${MQ_QUEUE_MANAGER}")
    val queueManager: String= ""

    @Value("\${MQ_USERNAME}")
    val mqUsername: String= ""

    @Value("\${MQ_PASSWORD}")
    val mqPassword: String= ""

}