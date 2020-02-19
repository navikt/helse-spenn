package no.nav.helse.spenn

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val vaultServiceUserBase = "/var/run/secrets/nais.io/service_user"
const val stsRestBaseUrl = "http://security-token-service"
const val ourIssuer = "ourissuer"

val vaultServiceUserBasePath: Path = Paths.get(vaultServiceUserBase)

fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(vaultServiceUserBasePath.resolve("username")),
    password = Files.readString(vaultServiceUserBasePath.resolve("password"))
)

fun readEnvironment() = Environment(
    simuleringServiceUrl = System.getenv("SIMULERING_SERVICE_URL"),
    stsSoapUrl = System.getenv("SECURITYTOKENSERVICE_URL"),
    aktorRegisteretBaseUrl = System.getenv("AKTORREGISTERET_BASE_URL"),
    auth = AuthEnvironment(
        acceptedAudience = System.getenv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE"),
        discoveryUrl = URL(System.getenv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL")),
        requiredGroup = System.getenv("API_ACCESS_REQUIREDGROUP")
    ),
    db = DbEnvironment(
        jdbcUrl = System.getenv("DATASOURCE_URL"),
        vaultPostgresMountpath = System.getenv("VAULT_POSTGRES_MOUNTPATH")
    ),
    mq = MqEnvironment(
        queueManager = System.getenv("MQ_QUEUE_MANAGER"),
        channel = System.getenv("MQ_CHANNEL"),
        hostname = System.getenv("MQ_HOSTNAME"),
        port = System.getenv("MQ_PORT").toInt(),
        user = System.getenv("MQ_USER"),
        password = System.getenv("MQ_PASSWORD"),
        oppdragQueueSend = System.getenv("OPPDRAG_QUEUE_SEND"),
        oppdragQueueMottak = System.getenv("OPPDRAG_QUEUE_MOTTAK"),
        avstemmingQueueSend = System.getenv("AVSTEMMING_QUEUE_SEND")
    ),
    kafka = KafkaEnvironment(
        bootstrapServersUrl = System.getenv("KAFKA_BOOTSTRAP_SERVERS"),
        truststorePath = System.getenv("NAV_TRUSTSTORE_PATH"),
        truststorePassword = System.getenv("NAV_TRUSTSTORE_PASSWORD")
    )
)

data class ServiceUser(
    val username: String,
    val password: String
)

data class Environment(
    val simuleringServiceUrl: String,
    val stsSoapUrl: String,
    val aktorRegisteretBaseUrl: String,
    val auth: AuthEnvironment,
    val db: DbEnvironment,
    val mq: MqEnvironment,
    val kafka: KafkaEnvironment
)

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

data class KafkaEnvironment(
    val appId: String = "spenn-1",
    val bootstrapServersUrl: String,
    val truststorePath: String?,
    val truststorePassword: String?,
    val plainTextKafka: Boolean = false,
    val offsetReset: Boolean = false,
    val timeStampMillis: Long = -1,
    val streamVedtak: Boolean = true
)
