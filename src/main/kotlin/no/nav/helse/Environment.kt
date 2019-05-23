package no.nav.helse

import org.springframework.beans.factory.annotation.Value

class Environment {

    val appId: String = "spenn-1"

    @Value("\${KAFKA_USERNAME}")
    lateinit var kafkaUsername: String

    @Value("\${KAFKA_PASSWORD}")
    lateinit var kafkaPassword: String

    @Value("\${STS_SOAP_USERNAME}")
    lateinit var stsUsername: String

    @Value("\${STS_SOAP_PASSWORD}")
    lateinit var stsPassword: String

    @Value("\${SECURITYTOKENSERVICE_URL}")
    lateinit var stsUrl: String

    @Value("\${KAFKA_BOOTSTRAP_SERVERS}")
    lateinit var bootstrapServersUrl: String

    @Value("\${NAV_TRUSTSTORE_PATH}")
    lateinit var navTruststorePath: String

    @Value("\${SPARKEL_BASE_URL:http://sparkel}")
    lateinit var sparkelBaseUrl: String

    @Value("\${SECURITY_TOKEN_SERVICE_REST_URL}")
    lateinit var stsRestUrl: String

    @Value("\${STS_REST_USERNAME}")
    lateinit var stsRestUsername: String

    @Value("\${STS_REST_PASSWORD}")
    lateinit var stsRestPassword: String

    @Value("\${PLAIN_TEXT_KAFKA}")
    lateinit var plainTextKafka: String

    @Value("\${NAV_TRUSTSTORE_PASSWORD}")
    lateinit var navTruststorePassword: String

    @Value("\${SIMULERING_SERVICE_URL}")
    lateinit var simuleringServiceUrl: String

    @Value("\${MQ_HOSTNAME}")
    lateinit var mqHostname: String

    @Value("\${MQ_CHANNEL}")
    lateinit var mqChannel: String

    @Value("\${MQ_PORT}")
    lateinit var mqPort: String

    @Value("\${MQ_QUEUE_MANAGER}")
    lateinit var queueManager: String

    @Value("\${MQ_USERNAME}")
    lateinit var mqUsername: String

    @Value("\${MQ_PASSWORD}")
    lateinit var mqPassword: String

}