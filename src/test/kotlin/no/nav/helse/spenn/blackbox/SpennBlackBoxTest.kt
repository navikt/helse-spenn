package no.nav.helse.spenn.blackbox

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigResolveOptions
import io.ktor.config.HoconApplicationConfig
import no.nav.helse.spenn.spenn
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.testcontainers.containers.*
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.MountableFile
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*

internal class SpennBlackBoxTest {

    @Test
    fun `spenn svarer på et utbetalingsbehov`() {
        val aktørId = "123456789"
        val fnr = "987654321"

        mockClient.settOppAktørregisteret(aktørId, fnr)
        mockClient.settOppStsMock()

        val sendtBehov = sendUtbetalingsbehov(aktørId)
        val mottattBehovMedLøsning = ventPåLøsning()

        val løsning = mottattBehovMedLøsning["@løsning"]
        assertTrue(løsning.hasNonNull("oppdragId"))



        println("------------------")
        println("http://host.testcontainer.internal:${soapMock.port}/ws/simulering")
        println("------------------")


        val løsningPåBehovUtenLøsning = (mottattBehovMedLøsning as ObjectNode).apply {
            remove("@løsning")
        }
        assertEquals(sendtBehov, løsningPåBehovUtenLøsning)
        Thread.sleep(600000)
    }

    private fun ventPåLøsning(timeout: Duration = Duration.ofSeconds(30)): JsonNode {
        KafkaConsumer(consumerProperties(bootstrapServers), StringDeserializer(), StringDeserializer()).use { consumer ->
            consumer.subscribe(listOf(behovTopic))
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
    "@behov": "Utbetaling",
    "sakskompleksId": "${UUID.randomUUID()}",
    "aktørId": "$aktørId",
    "organisasjonsnummer": "666",
    "saksbehandler": "Z999999",
    "utbetalingsreferanse": "foobar",
    "maksdato": "2019-01-01",
    "utbetalingslinjer": []
}""".trimIndent()
            .also { behov ->
                KafkaProducer(producerProperties(bootstrapServers), StringSerializer(), StringSerializer()).use {
                    it.send(ProducerRecord(behovTopic, behov))
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

    //////////


    private fun MockServerClient.settOppStsMock() {
        this.`when`(request()
            .withPath("/soapsts")
        ).respond(HttpResponse.response()
            .withStatusCode(200)
            .withBody(sts_response))
    }

    ///////////

    private fun MockServerClient.settOppAktørregisteret(aktørId: String, fnr: String) {
        this.`when`(request()
                .withPath("/rest/v1/sts/token")
                .withQueryStringParameter("grant_type", "client_credentials")
                .withQueryStringParameter("scope", "openid")
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody("""
{
    "access_token": "a_token",
    "token_type": "Bearer",
    "expires_in": 3600
}""".trimIndent()))

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

        this.`when`(request()
                .withPath("/api/v1/identer")
                .withQueryStringParameter("gjeldende", "true")
                .withHeader("Nav-Personidenter", aktørId)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody(aktørregisteret_response))
    }

    private companion object {
        private const val behovTopic = "privat-helse-sykepenger-behov"

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
        private const val VaultHostname = "localhost" //""vault"
        private const val VaultPort = 8200
        private const val VaultRootToken = "token123"
        private const val VaultPostgresMountPath = "postgresql/preprod-fss"

        private const val OidcHostname = "simple-oidc-provider"
        private const val OidcPort = 9000

        private const val MockServerHostname = "mockserver"
        private const val MockServerPort = MockServerContainer.PORT

        private val soapMock = SoapMock()

        private val objectMapper = ObjectMapper()
        private class PostgreContainer(dockerImageName: String) : PostgreSQLContainer<PostgreContainer>(dockerImageName) {
            init {
                withInitScript("an_init_script")
            }

            override fun runInitScriptIfRequired() {
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "create role \"$databaseName-admin\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "create role \"$databaseName-user\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "create role \"$databaseName-readonly\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "GRANT \"$databaseName-readonly\" TO \"$databaseName-user\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "GRANT \"$databaseName-user\" TO \"$databaseName-admin\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-admin\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-user\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "GRANT CONNECT ON DATABASE \"$databaseName\" TO \"$databaseName-readonly\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select, usage on sequences to \"$databaseName-readonly\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select on tables to \"$databaseName-readonly\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant select, insert, update, delete, truncate on tables to \"$databaseName-user\";")
                ScriptUtils.executeDatabaseScript(databaseDelegate, "an_init_script", "alter default privileges for role \"$databaseName-admin\" in schema \"public\" grant all privileges on tables to \"$databaseName-admin\";")
            }
        }

        private class DockerContainer(dockerImageName: String) : GenericContainer<DockerContainer>(dockerImageName)

        private lateinit var bootstrapServers: String

        private lateinit var mockClient: MockServerClient

        @BeforeAll
        @JvmStatic
        private fun setupCluster() {
            soapMock.start()
            val network = Network.newNetwork()

            val postgre = setupPostgre(network)
            postgre.withExposedPorts(5432)
            postgre.start()

            val zookeeper = setupZooKeeper(network)
            zookeeper.start()

            val kafka = setupKafka(network)
            kafka.start()

            setupTopic(kafka)
            bootstrapServers = kafka.bootstrapServers

            val mq = setupMq(network)
            mq.start()

            val vault = setupVault(network)
            vault.withExposedPorts(8200)
            vault.start()

            val oidcContainer = setupOidc(network)
            val mockServer = setupMockServer(network)

            oidcContainer.start()
            mockServer.start()

            setupPostgreVault(vault)

            val spenn = setupSpenn(network)
            spenn.start()

            mockClient = MockServerClient(mockServer.containerIpAddress, mockServer.serverPort)
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

        private fun setupZooKeeper(network: Network): DockerContainer {
            return DockerContainer(ZooKeeperImage)
                    .withNetwork(network)
                    .withNetworkAliases(ZooKeeperHostname)
                    .withExposedPorts(ZooKeeperPort)
                    .withEnv("ZOOKEEPER_CLIENT_PORT", "$ZooKeeperPort")
                    .waitingFor(Wait.forListeningPort())
        }

        private fun setupKafka(network: Network): KafkaContainer {
            return KafkaContainer("5.1.0")
                    .withNetwork(network)
                    .withNetworkAliases(KafkaHostname)
                    .withExternalZookeeper("$ZooKeeperHostname:$ZooKeeperPort")
                    .withLogConsumer { print("kafka: " + it.utf8String) }
                    .waitingFor(Wait.forListeningPort())
        }

        private fun setupTopic(kafkaContainer: KafkaContainer) {
            kafkaContainer.execInContainer("kafka-topics", "--create",
                            "--if-not-exists",
                            "--zookeeper",
                            "$ZooKeeperHostname:$ZooKeeperPort",
                            "--partitions",
                            "1",
                            "--replication-factor",
                            "1",
                            "--topic",
                            behovTopic).print()
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
            vaultContainer.execInContainer("vault", "write", "$VaultPostgresMountPath/config/$SpennDatabase", "plugin_name=postgresql-database-plugin", "allowed_roles=$SpennDatabase-admin,$SpennDatabase-user", "connection_url=postgresql://{{username}}:{{password}}@$PostgresHostname:$PostgresPort?sslmode=disable", "username=$PostgresRootUsername", "password=$PostgresRootPassword")
                    .print()
            vaultContainer.execInContainer("vault", "write", "$VaultPostgresMountPath/roles/$SpennDatabase-admin", "db_name=$SpennDatabase", "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"$SpennDatabase-admin\" TO \"{{name}}\";", "default_ttl=5m", "max_ttl=5m")
                    .print()
            vaultContainer.execInContainer("vault", "write", "$VaultPostgresMountPath/roles/$SpennDatabase-user", "db_name=$SpennDatabase", "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"$SpennDatabase-user\" TO \"{{name}}\";", "default_ttl=5m", "max_ttl=5m")
                    .print()
        }

        private fun setupOidc(network: Network): DockerContainer {
            return DockerContainer("qlik/simple-oidc-provider")
                    .withNetwork(network)
                    .withNetworkAliases(OidcHostname)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-config.json"), "/oidc/config.json")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-users.json"), "/oidc/users.json")
                    .withEnv("USERS_FILE", "/oidc/users.json")
                    .withEnv("CONFIG_FILE", "/oidc/config.json")
        }

        private fun setupMockServer(network: Network): MockServerContainer {
            return MockServerContainer()
                .withNetwork(network)
                .withNetworkAliases(MockServerHostname)
                .waitingFor(Wait.forListeningPort())
        }

        object SpennContainer : GenericContainer<SpennContainer>(ImageFromDockerfile()
            .withFileFromPath(".", Paths.get(".")))


        private class LocalSpenn() {
            val envMap = mutableMapOf<String,String>()
            fun withEnv(name: String, value:String) : LocalSpenn {
                envMap[name] = value
                return this
            }

            fun start() {
                var confTxt = this.javaClass.getResource("/application.conf").readText()
                envMap.forEach {
                    val regexp = "\\\$\\{\\??${it.key}}"
                    confTxt = confTxt.replace(regexp.toRegex(), "\"${it.value}\"")
                }
                val conf = ConfigFactory.parseString(confTxt)
                    .resolve(ConfigResolveOptions.defaults())
                val appConfig = HoconApplicationConfig(conf)
                spenn(appConfig)
            }
        }

        private fun localSpenn() {

        }


        private fun setupSpenn(network: Network) = LocalSpenn() //SpennContainer
            //.withNetwork(network)
            .withEnv("VAULT_ADDR", "http://$VaultHostname:$VaultPort")
            .withEnv("VAULT_TOKEN", VaultRootToken)
            .withEnv("MQ_HOSTNAME", MqHostname)
            .withEnv("MQ_PORT", "$MqPort")
            .withEnv("SECURITYTOKENSERVICE_URL", "http://$MockServerHostname:$MockServerPort/soapsts")
            .withEnv("SECURITY_TOKEN_SERVICE_REST_URL", "http://$MockServerHostname:$MockServerPort")
            .withEnv("AKTORREGISTERET_BASE_URL", "http://$MockServerHostname:$MockServerPort")
            .withEnv("SIMULERING_SERVICE_URL", "http://host.testcontainer.internal:${soapMock.port}/ws/simulering")
            .withEnv("KAFKA_BOOTSTRAP_SERVERS", "$KafkaHostname:$KafkaPort")
            .withEnv("KAFKA_USERNAME", "foo")
            .withEnv("KAFKA_PASSWORD", "bar")
            .withEnv("PLAIN_TEXT_KAFKA", "true")
            .withEnv("NAV_TRUSTSTORE_PATH", "foo")
            .withEnv("NAV_TRUSTSTORE_PASSWORD", "bar")
            .withEnv("STS_SOAP_USERNAME", "foo")
            .withEnv("STS_SOAP_PASSWORD", "bar")
            .withEnv("DATASOURCE_URL", "jdbc:postgresql://$PostgresHostname:$PostgresPort/$SpennDatabase")
            .withEnv("DATASOURCE_VAULT_ENABLED", "true")
            .withEnv("DATASOURCE_VAULT_MOUNTPATH", VaultPostgresMountPath)
            .withEnv("SCHEDULER_ENABLED", "true")
            .withEnv("SCHEDULER_TASKS_SIMULERING", "true")
            .withEnv("SCHEDULER_TASKS_OPPDRAG", "true")
            .withEnv("SCHEDULER_TASKS_AVSTEMMING", "true")
            .withEnv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE", "audience")
            .withEnv(
                "NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL",
                "http://$OidcHostname:$OidcPort/.well-known/openid-configuration"
            )
            //.waitingFor(Wait.forListeningPort())
            //.withLogConsumer { print("spenn: " + it.utf8String) }
    }
}

private fun Container.ExecResult.print() {
    println("stdout: ${this.stdout.replace("\n", "")}")
    println("stderr: ${this.stderr.replace("\n", "")}")
}


