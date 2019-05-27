package no.nav.helse.spenn

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

    @Value("\${PLAIN_TEXT_KAFKA:false}")
    val plainTextKafka: String= ""

    @Value("\${NAV_TRUSTSTORE_PASSWORD}")
    val navTruststorePassword: String= ""

    @Value("\${SIMULERING_SERVICE_URL}")
    val simuleringServiceUrl: String= ""

}