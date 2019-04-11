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
        val oppdragDBNavn: String = getEnvVar("OPPDRAG_DB_NAVN")

)

private fun getEnvVarOptional(varName: String, defaultValue: String? = null) = System.getenv(varName) ?: defaultValue

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        getEnvVarOptional(varName, defaultValue) ?: throw Exception("mangler verdi for $varName")