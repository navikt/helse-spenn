package no.nav.helse

data class Environment(
        val appId: String = "spenn-1",
        val kafkaUsername: String? = getEnvVarOptional("SERVICEUSER_USERNAME"),
        val kafkaPassword: String? = getEnvVarOptional("SERVICEUSER_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val navTruststorePath: String? = getEnvVarOptional("NAV_TRUSTSTORE_PATH"),
        val plainTextKafka: String? = getEnvVarOptional("PLAIN_TEXT_KAFKA"),
        val navTruststorePassword: String? = getEnvVarOptional("NAV_TRUSTSTORE_PASSWORD")

)

private fun getEnvVarOptional(varName: String, defaultValue: String? = null) = System.getenv(varName) ?: defaultValue

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        getEnvVarOptional(varName, defaultValue) ?: throw Exception("mangler verdi for $varName")