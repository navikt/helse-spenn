package no.nav.helse.spenn

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val vaultServiceUserBase = "/var/run/secrets/nais.io/service_user"
const val stsRestBaseUrl = "http://security-token-service"
const val ourIssuer = "ourissuer"

const val simuleringServiceUrlKey = "SIMULERING_SERVICE_URL"
const val stsSoapUrlKey = "SECURITYTOKENSERVICE_URL"
const val aktorRegisteretBaseUrlKey = "AKTORREGISTERET_BASE_URL"
const val acceptedAudienceKey = "NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE"
const val discoveryUrlKey = "NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL"
const val requiredGroupKey = "API_ACCESS_REQUIREDGROUP"
const val datasourceUrlKey = "DATASOURCE_URL"
const val vaultPostgresMountpathKey = "VAULT_POSTGRES_MOUNTPATH"
const val mqQueueManagerKey = "MQ_QUEUE_MANAGER"
const val mqChannelKey = "MQ_CHANNEL"
const val mqHostnameKey = "MQ_HOSTNAME"
const val mqPortKey = "MQ_PORT"
const val mqUserKey = "MQ_USER"
const val mqPasswordKey = "MQ_PASSWORD"
const val mqOppdragQueueSendKey = "OPPDRAG_QUEUE_SEND"
const val mqOppdragQueueMottakKey = "OPPDRAG_QUEUE_MOTTAK"
const val mqAvvstemmingQueueSendKey = "AVSTEMMING_QUEUE_SEND"

val vaultServiceUserBasePath: Path = Paths.get(vaultServiceUserBase)

fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(vaultServiceUserBasePath.resolve("username")),
    password = Files.readString(vaultServiceUserBasePath.resolve("password"))
)

fun readEnvironment() = Environment(System.getenv())

data class ServiceUser(
    val username: String,
    val password: String
)

data class Environment(
    val raw: Map<String, String>,
    val simuleringServiceUrl: String,
    val stsSoapUrl: String,
    val aktorRegisteretBaseUrl: String,
    val auth: AuthEnvironment,
    val db: DbEnvironment,
    val mq: MqEnvironment
) {
    constructor (raw: Map<String, String>) : this(
        raw = raw,
        simuleringServiceUrl = raw.hent(simuleringServiceUrlKey),
        stsSoapUrl = raw.hent(stsSoapUrlKey),
        aktorRegisteretBaseUrl = raw.hent(aktorRegisteretBaseUrlKey),
        auth = AuthEnvironment(
            acceptedAudience = raw.hent(acceptedAudienceKey),
            discoveryUrl = URL(raw.hent(discoveryUrlKey)),
            requiredGroup = raw.hent(requiredGroupKey)
        ),
        db = DbEnvironment(
            jdbcUrl = raw.hent(datasourceUrlKey),
            vaultPostgresMountpath = raw.hent(vaultPostgresMountpathKey)
        ),
        mq = MqEnvironment(
            queueManager = raw.hent(mqQueueManagerKey),
            channel = raw.hent(mqChannelKey),
            hostname = raw.hent(mqHostnameKey),
            port = raw.hent(mqPortKey).toInt(),
            user = raw.hent(mqUserKey),
            password = raw.hent(mqPasswordKey),
            oppdragQueueSend = raw.hent(mqOppdragQueueSendKey),
            oppdragQueueMottak = raw.hent(mqOppdragQueueMottakKey),
            avstemmingQueueSend = raw.hent(mqAvvstemmingQueueSendKey)
        )
    )
}

private fun Map<String, String>.hent(key: String): String = requireNotNull(this[key]) { "$key mangler i env" }

data class AuthEnvironment(
    val acceptedAudience: String,
    val discoveryUrl: URL,
    val requiredGroup: String
)

data class DbEnvironment(
    val jdbcUrl: String,
    val vaultPostgresMountpath: String
)

data class MqEnvironment(
    val queueManager: String,
    val channel: String,
    val hostname: String,
    val port: Int,
    val user: String,
    val password: String,
    val oppdragQueueSend: String,
    val oppdragQueueMottak: String,
    val avstemmingQueueSend: String
)
