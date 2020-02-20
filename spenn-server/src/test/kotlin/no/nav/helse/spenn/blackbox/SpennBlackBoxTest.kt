package no.nav.helse.spenn.blackbox

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.Environment
import no.nav.helse.spenn.ServiceUser
import no.nav.helse.spenn.acceptedAudienceKey
import no.nav.helse.spenn.aktorRegisteretBaseUrlKey
import no.nav.helse.spenn.blackbox.mq.OppdragMock
import no.nav.helse.spenn.blackbox.soap.SoapMock
import no.nav.helse.spenn.datasourceUrlKey
import no.nav.helse.spenn.discoveryUrlKey
import no.nav.helse.spenn.mqAvvstemmingQueueSendKey
import no.nav.helse.spenn.mqChannelKey
import no.nav.helse.spenn.mqHostnameKey
import no.nav.helse.spenn.mqOppdragQueueMottakKey
import no.nav.helse.spenn.mqOppdragQueueSendKey
import no.nav.helse.spenn.mqPasswordKey
import no.nav.helse.spenn.mqPortKey
import no.nav.helse.spenn.mqQueueManagerKey
import no.nav.helse.spenn.mqUsernameKey
import no.nav.helse.spenn.requiredGroupKey
import no.nav.helse.spenn.setUpAndLaunchApplication
import no.nav.helse.spenn.simuleringServiceUrlKey
import no.nav.helse.spenn.stsSoapUrlKey
import no.nav.helse.spenn.vaultPostgresMountpathKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.MountableFile
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.HashMap
import java.util.Properties
import java.util.UUID

internal class SpennBlackBoxTest {

    @Test
    fun `spenn svarer på et utbetalingsbehov`() {
        val aktørId = "123456789"
        val fnr = "987654321"

        mockClient.settOppAktørregisteret(aktørId, fnr)

        sendUtbetalingsbehov(aktørId)

        println("------------------")
        println("http://host.testcontainers.internal:${soapMock.httpPort}/ws/simulering")
        println("https://host.testcontainers.internal:${soapMock.httpsPort}/ws/simulering")
        println("------------------")

        assertTimeoutPreemptively(Duration.ofMinutes(5)) {
            while (mockOppdrag.messagesReceived.isEmpty()) {
                Thread.sleep(100)
            }
        }
    }

    private fun ventPåLøsning(timeout: Duration = Duration.ofSeconds(30)): JsonNode {
        KafkaConsumer(
            consumerProperties(bootstrapServers),
            StringDeserializer(),
            StringDeserializer()
        ).use { consumer ->
            consumer.subscribe(listOf(rapidTopic))
            val endTime = Instant.now() + timeout

            while (Instant.now() < endTime) {
                val løsning = consumer.poll(Duration.ofMillis(0))
                    .map { objectMapper.readTree(it.value()) }
                    .firstOrNull { it.hasNonNull("@løsning") }

                if (løsning != null)
                    return løsning
            }
            throw RuntimeException("forventet å få en løsning i løpet av ${timeout.toSeconds()} sekunder")
        }
    }

    private fun sendUtbetalingsbehov(aktørId: String): JsonNode {
        return """
{
    "@behov": ["Utbetaling"],
    "sakskompleksId": "${UUID.randomUUID()}",
    "aktørId": "$aktørId",
    "organisasjonsnummer": "666666666",
    "utbetalingsreferanse": "foobar",
    "saksbehandler": "Z999999",
    "maksdato": "2019-01-01",
    "utbetalingslinjer": [
        {
            "grad": 100,
            "dagsats": 1338,
            "fom": "2018-12-10",
            "tom": "2018-12-18"
        }
    ]
}""".trimIndent()
            .also { behov ->
                KafkaProducer(producerProperties(bootstrapServers), StringSerializer(), StringSerializer()).use {
                    it.send(ProducerRecord(rapidTopic, behov))
                }
            }
            .let { objectMapper.readTree(it) }
    }

    private fun consumerProperties(bootstrapServer: String): MutableMap<String, Any>? {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spennComponentTest")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    }

    private fun producerProperties(bootstrapServer: String) =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, "0")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        }

    private fun MockServerClient.settOppAktørregisteret(aktørId: String, fnr: String) {
        this.`when`(
            request()
                .withPath("/rest/v1/sts/token")
                .withQueryStringParameter("grant_type", "client_credentials")
                .withQueryStringParameter("scope", "openid")
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(
                    """
{
    "access_token": "a_token",
    "token_type": "Bearer",
    "expires_in": 3600
}""".trimIndent()
                )
        )

        val aktørregisteret_response = """
{
  "$aktørId": {
    "identer": [
      {
        "ident": "$aktørId",
        "identgruppe": "AktoerId",
        "gjeldende": true
      },
      {
        "ident": "$fnr",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()

        this.`when`(
            request()
                .withPath("/api/v1/identer")
                .withQueryStringParameter("gjeldende", "true")
                .withHeader("Nav-Personidenter", aktørId)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody(aktørregisteret_response)
        )
    }

    private companion object {
        private const val rapidTopic = "helse-rapid-v1"

        private const val PostgresImage = "postgres:11-alpine"
        private const val PostgresHostname = "postgres"
        private const val PostgresPort = 5432
        private const val PostgresRootUsername = "postgres"
        private const val PostgresRootPassword = "postgres"
        private const val SpennDatabase = "helse-spenn-oppdrag"

        private const val ZooKeeperImage = "confluentinc/cp-zookeeper:5.1.0"
        private const val ZooKeeperHostname = "zookeeper"
        private const val ZooKeeperPort = 2181

        private const val KafkaHostname = "kafka"
        private const val KafkaPort = 9092

        private const val MqHostname = "mq"
        private const val MqPort = 1414
        private const val MqQueueMananagerName = "QM1"

        private const val VaultImage = "vault:1.1.0"
        private const val VaultHostname = "vault"
        private const val VaultPort = 8200
        private const val VaultRootToken = "token123"
        private const val VaultPostgresMountPath = "postgresql/preprod-fss"

        private const val OidcHostname = "simple-oidc-provider"
        private const val OidcPort = 9000

        private const val MockServerHostname = "mockserver"
        private const val MockServerPort = MockServerContainer.PORT

        private val soapMock = SoapMock()


        private val objectMapper = ObjectMapper()

        private class PostgreContainer(dockerImageName: String) :
            PostgreSQLContainer<PostgreContainer>(dockerImageName) {
            init {
                withInitScript("an_init_script")
            }

            override fun runInitScriptIfRequired() {
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "create role \"$databaseName-admin\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "create role \"$databaseName-user\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "create role \"$databaseName-readonly\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "GRANT \"$databaseName-readonly\" TO \"$databaseName-user\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "GRANT \"$databaseName-user\" TO \"$databaseName-admin\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-admin\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-user\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-readonly\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select, usage on sequences to \"$databaseName-readonly\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select on tables to \"$databaseName-readonly\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select, insert, update, delete, truncate on tables to \"$databaseName-user\";"
                )
                ScriptUtils.executeDatabaseScript(
                    databaseDelegate,
                    "an_init_script",
                    "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant all privileges on tables to \"$databaseName-admin\";"
                )
            }
        }

        private class DockerContainer(dockerImageName: String) : GenericContainer<DockerContainer>(dockerImageName)

        private lateinit var bootstrapServers: String

        private lateinit var mockClient: MockServerClient

        private lateinit var mockOppdrag: OppdragMock

        @BeforeAll
        @JvmStatic
        private fun setupCluster() {
            soapMock.start()

            //val network = Network.SHARED
            val network = Network.newNetwork()

            val postgre = setupPostgre(network)
            postgre.start()

            val kafka = setupKafka(network)

            kafka.portBindings = listOf("9092:9092")
            kafka.withNetworkMode("host")

            kafka.start()

            setupTopic(kafka)
            bootstrapServers = kafka.bootstrapServers

            println(bootstrapServers)

            val mq = setupMq(network)
            mq.start()

            println("MQ ADMIN = ${mq.getMappedPort(9443)}")

            val vault = setupVault(network)
            vault.start()

            val oidcContainer = setupOidc(network)
            val mockServer = setupMockServer(network)

            oidcContainer.start()
            mockServer.start()

            setupPostgreVault(vault)
            kafka.networkAliases
            postgre.networkAliases
            mockServer.networkAliases

            mockClient = MockServerClient(mockServer.containerIpAddress, mockServer.serverPort)

            mockOppdrag = OppdragMock(
                host = "localhost",
                port = mq.getMappedPort(MqPort),
                channel = "DEV.ADMIN.SVRCONN",
                queueManager = "QM1",
                user = "admin",
                password = "passw0rd",
                oppdragQueue = "DEV.QUEUE.2"
            ).also {
                it.listen()
            }

            val spenn = setupSpenn(network)
            //val spenn = setupLocalSpenn(listOf(postgre, kafka, mq, vault, oidcContainer, mockServer))
            spenn.start()

        }

        @AfterAll
        @JvmStatic
        private fun shutdownCluster() {
            soapMock.close()
        }

        private fun setupPostgre(network: Network): PostgreContainer {
            return PostgreContainer(PostgresImage)
                .withNetwork(network)
                .withNetworkAliases(PostgresHostname)
                .withDatabaseName(SpennDatabase)
                .withUsername(PostgresRootUsername)
                .withPassword(PostgresRootPassword)
        }

        private fun setupKafka(network: Network): KafkaContainer {
            return KafkaContainer("5.1.0")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withNetworkMode("host")
                .withLogConsumer { print("kafka: " + it.utf8String) }
                .waitingFor(Wait.forListeningPort())
        }

        private fun setupTopic(kafkaContainer: KafkaContainer) {
            kafkaContainer.execInContainer(
                "kafka-topics", "--create",
                "--if-not-exists",
                "--zookeeper",
                "localhost:$ZooKeeperPort",
                "--partitions",
                "1",
                "--replication-factor",
                "1",
                "--topic",
                rapidTopic
            ).print()
        }

        private fun setupMq(network: Network): DockerContainer {
            return DockerContainer("ibmcom/mq:latest")
                .withNetwork(network)
                .withNetworkAliases(MqHostname)
                .withEnv("LICENSE", "accept")
                .withEnv("MQ_QMGR_NAME", MqQueueMananagerName)
        }

        private fun setupVault(network: Network): DockerContainer {
            return DockerContainer(VaultImage)
                .withNetwork(network)
                .withNetworkAliases(VaultHostname)
                .withEnv("VAULT_ADDR", "http://127.0.0.1:$VaultPort")
                .withEnv("VAULT_TOKEN", VaultRootToken)
                .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VaultRootToken)
                .withLogConsumer { print("vault: " + it.utf8String) }
        }

        private fun setupPostgreVault(vaultContainer: DockerContainer) {
            vaultContainer.execInContainer("vault", "secrets", "enable", "-path=$VaultPostgresMountPath", "database")
                .print()
            vaultContainer.execInContainer(
                "vault",
                "write",
                "$VaultPostgresMountPath/config/$SpennDatabase",
                "plugin_name=postgresql-database-plugin",
                "allowed_roles=$SpennDatabase-admin,$SpennDatabase-user",
                "connection_url=postgresql://{{username}}:{{password}}@$PostgresHostname:$PostgresPort?sslmode=disable",
                "username=$PostgresRootUsername",
                "password=$PostgresRootPassword"
            )
                .print()
            vaultContainer.execInContainer(
                "vault",
                "write",
                "$VaultPostgresMountPath/roles/$SpennDatabase-admin",
                "db_name=$SpennDatabase",
                "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"$SpennDatabase-admin\" TO \"{{name}}\";",
                "default_ttl=5m",
                "max_ttl=5m"
            )
                .print()
            vaultContainer.execInContainer(
                "vault",
                "write",
                "$VaultPostgresMountPath/roles/$SpennDatabase-user",
                "db_name=$SpennDatabase",
                "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"$SpennDatabase-user\" TO \"{{name}}\";",
                "default_ttl=5m",
                "max_ttl=5m"
            )
                .print()
        }

        private fun setupOidc(network: Network): DockerContainer {
            return DockerContainer("qlik/simple-oidc-provider")
                .withNetwork(network)
                .withNetworkAliases(OidcHostname)
                .withFileSystemBind(
                    Paths.get("..").resolve("compose").resolve("oidc").toAbsolutePath().toString(),
                    "/oidc"
                )
                .withEnv("USERS_FILE", "/oidc/users.json")
                .withEnv("CONFIG_FILE", "/oidc/config.json")
        }

        private fun setupMockServer(network: Network): MockServerContainer {
            return MockServerContainer()
                .withNetwork(network)
                .withNetworkAliases(MockServerHostname)
                .waitingFor(Wait.forListeningPort())
        }

        object SpennContainer : GenericContainer<SpennContainer>(
            ImageFromDockerfile()
                .withFileFromPath(".", Paths.get(".."))
        )


        private class LocalSpenn(val envMap: Map<String, String>) {

            fun start() {
                Thread(this::dorun).run()
            }

            @KtorExperimentalAPI
            private fun dorun() {
                setUpAndLaunchApplication(Environment(envMap), ServiceUser("username", "password"))
            }
        }


        data class HostInfo(
            val hostname: String,
            val port: Int
        ) {
            val http get() = "http://$hostname:$port"
            val https get() = "https://$hostname:$port"
            val hostPort get() = "$hostname:$port"
        }

        fun setupEnv(
            additionalEnv: Map<String, String> = mapOf(),
            hostResolver: (String, Int) -> HostInfo
        ) = mapOf(
            simuleringServiceUrlKey to "${hostResolver("localhost", soapMock.httpsPort).https}/ws/simulering",
            stsSoapUrlKey to "${hostResolver("localhost", soapMock.httpsPort).https}/ws/SecurityTokenService",
            aktorRegisteretBaseUrlKey to hostResolver(MockServerHostname, MockServerPort).http,
            acceptedAudienceKey to "audience",
            discoveryUrlKey to "${hostResolver(OidcHostname, OidcPort).http}/.well-known/openid-configuration",
            requiredGroupKey to "",
            datasourceUrlKey to "jdbc:postgresql://${hostResolver(PostgresHostname, PostgresPort).hostPort}/$SpennDatabase",
            vaultPostgresMountpathKey to VaultPostgresMountPath,
            mqQueueManagerKey to "",
            mqChannelKey to "",
            mqHostnameKey to hostResolver(MqHostname, MqPort).hostname,
            mqPortKey to hostResolver(MqHostname, MqPort).port.toString(),
            mqUsernameKey to "",
            mqPasswordKey to "",
            mqOppdragQueueSendKey to "",
            mqOppdragQueueMottakKey to "",
            mqAvvstemmingQueueSendKey to "",
            "KAFKA_BOOTSTRAP_SERVERS" to bootstrapServers,
            "NAV_TRUSTSTORE_PATH" to "/tmp/keystore.p12",
            "NAV_TRUSTSTORE_PASSWORD" to soapMock.keystorePassword
        ) + additionalEnv

        private fun setupLocalSpenn(containers: List<GenericContainer<*>>): LocalSpenn {

            System.setProperty("javax.net.ssl.trustStore", soapMock.keystorePath.toAbsolutePath().toString())
            System.setProperty("javax.net.ssl.trustStorePassword", soapMock.keystorePassword)

            fun toLocalPort(hostname: String, port: Int): Int = containers
                .filter { it.getNetworkAliases().contains(hostname) }
                .map { it.getMappedPort(port) }
                .first()
                .also { println("Port for $hostname=$port, mappedPort=$it") }

            fun baseUrlForContainer(hostname: String, port: Int) = when (hostname) {
                "localhost" -> HostInfo(hostname = "localhost", port = port)
                else -> HostInfo(hostname = "localhost", port = toLocalPort(hostname, port))
            }

            return LocalSpenn(setupEnv(hostResolver = ::baseUrlForContainer))
        }


        private fun setupSpenn(network: Network): SpennContainer {
            fun hostResolverContainer(hostname: String, port: Int): HostInfo = when (hostname) {
                "localhost" -> HostInfo(hostname = "host.testcontainers.internal", port = port)
                else -> HostInfo(hostname = hostname, port = port)
            }
            return SpennContainer
                .withNetwork(network)
                .also { setupEnv(hostResolver = ::hostResolverContainer) }
                .withEnv("KAFKA_BOOTSTRAP_SERVERS", "$KafkaHostname:$KafkaPort")
                .waitingFor(Wait.forListeningPort())
                .withCopyFileToContainer(MountableFile.forHostPath(soapMock.keystorePath), "/tmp/keystore.p12")
                .withLogConsumer { print("spenn: " + it.utf8String) }
        }
    }
}

private fun Container.ExecResult.print() {
    println("stdout: ${this.stdout.replace("\n", "")}")
    println("stderr: ${this.stderr.replace("\n", "")}")
}


