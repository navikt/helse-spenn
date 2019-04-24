package no.nav.helse

data class Environment(
        val appId: String = "spenn-1",
        val kafkaUsername: String? = getEnvVarOptional("SERVICEUSER_USERNAME"),
        val kafkaPassword: String? = getEnvVarOptional("SERVICEUSER_PASSWORD"),
        val stsUsername: String? = getEnvVarOptional("SERVICEUSER_USERNAME"),
        val stsPassword: String? = getEnvVarOptional("SERVICEUSER_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val navTruststorePath: String? = getEnvVarOptional("NAV_TRUSTSTORE_PATH"),
        val sparkelBaseUrl: String = getEnvVar("SPARKEL_BASE_URL", "http://sparkel"),
        val stsRestUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL"),
        val plainTextKafka: String? = getEnvVarOptional("PLAIN_TEXT_KAFKA"),
        val navTruststorePassword: String? = getEnvVarOptional("NAV_TRUSTSTORE_PASSWORD"),
        val oppdragDBNavn: String = getEnvVar("OPPDRAG_DB_NAVN"),
        val stsUrl: String? = getEnvVarOptional("SECURITYTOKENSERVICE_URL"),
        val oppdragServiceUrl: String? = getEnvVarOptional("OPPDRAG_SERVICE_URL"),
        val mqHostname: String? = getEnvVarOptional("MQ_HOSTNAME", "localhost"),
        val mqChannel: String? = getEnvVarOptional("MQ_CHANNEL", "who_knows"),
        val mqPort: String = getEnvVar("MQ_PORT", "1414"),
        val queueManager: String? = getEnvVarOptional("MQ_QUEUE_MANAGER", "MQQM"),
        val mqUsername: String = getEnvVar("MQ_USERNAME", "rabbit"),
        val mqPassword: String = getEnvVar("MQ_PASSWORD", "rabbit")

)

private fun getEnvVarOptional(varName: String, defaultValue: String? = null) = System.getenv(varName) ?: defaultValue

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        getEnvVarOptional(varName, defaultValue) ?: throw Exception("mangler verdi for $varName")