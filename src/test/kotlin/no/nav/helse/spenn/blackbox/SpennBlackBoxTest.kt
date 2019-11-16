package no.nav.helse.spenn.blackbox

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.testcontainers.containers.*
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.utility.MountableFile
import java.time.Duration
import java.time.Instant
import java.util.*

internal class SpennBlackBoxTest {

    @Test
    fun `spenn svarer på et utbetalingsbehov`() {
        val aktørId = "123456789"
        val fnr = "987654321"

        mockClient.settOppAktørregisteret(aktørId, fnr)

        val sendtBehov = sendUtbetalingsbehov(aktørId)
        val mottattBehovMedLøsning = ventPåLøsning()

        val løsning = mottattBehovMedLøsning["@løsning"]
        assertTrue(løsning.hasNonNull("oppdragId"))

        val løsningPåBehovUtenLøsning = (mottattBehovMedLøsning as com.fasterxml.jackson.databind.node.ObjectNode).apply {
            remove("@løsning")
        }
        assertEquals(sendtBehov, løsningPåBehovUtenLøsning)
    }

    private fun ventPåLøsning(timeout: Duration = Duration.ofSeconds(30)): JsonNode {
        return KafkaConsumer(consumerProperties(bootstrapServers), StringDeserializer(), StringDeserializer()).use { consumer ->
            consumer.subscribe(listOf(behovTopic))

            var løsning: JsonNode? = null
            val endTime = Instant.now().plusMillis(timeout.toMillis())

            while (Instant.now() < endTime && løsning == null) {
                løsning = consumer.poll(Duration.ofMillis(0)).map {
                    objectMapper.readTree(it.value())
                }.firstOrNull { it.hasNonNull("@løsning") }
            }

            løsning ?: throw RuntimeException("forventet å få en løsning i løpet av ${timeout.toSeconds()} sekunder")
        }
    }

    private fun sendUtbetalingsbehov(aktørId: String): JsonNode {
        return """
{
    "@behov": "Utbetaling",
    "sakskompleksId": "${UUID.randomUUID().toString()}",
    "aktørId": "$aktørId",
    "organisasjonsnummer": "666",
    "saksbehandler": "Z999999",
    "utbetalingsreferanse": "foobar",
    "maksdato": "2019-01-01",
    "utbetalingslinjer": []
}""".trimIndent().also { behov ->
            KafkaProducer(producerProperties(bootstrapServers), StringSerializer(), StringSerializer()).use {
                it.send(ProducerRecord(behovTopic, behov))
            }
        }.let {
            objectMapper.readTree(it)
        }
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
        private const val PostgresInitScript = "/blackbox_test-init.sql"
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

        private const val OidcHostname = "simple-oidc-provider"
        private const val OidcPort = 9000

        private const val MockServerHostname = "mockserver"
        private const val MockServerPort = MockServerContainer.PORT

        private val objectMapper = ObjectMapper()
        private class PostgreContainer(dockerImageName: String) : PostgreSQLContainer<PostgreContainer>(dockerImageName) {
            override fun runInitScriptIfRequired() {
                ScriptUtils.executeDatabaseScript(databaseDelegate, PostgresInitScript, PostgresInitScript.readResource())
            }

            private fun String.readResource() =
                    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: throw RuntimeException("did not find resource <$this>")
        }

        private class ZooKeeperContainer(dockerImageName: String) : GenericContainer<ZooKeeperContainer>(dockerImageName)

        private class VaultContainer(dockerImageName: String) : GenericContainer<VaultContainer>(dockerImageName)

        private class KafkaKontainer(dockerImageName: String): GenericContainer<KafkaKontainer>(dockerImageName)

        private class MqContainer(dockerImageName: String): GenericContainer<MqContainer>(dockerImageName)

        private class OidcContainer(dockerImageName: String): GenericContainer<OidcContainer>(dockerImageName)
        private class SpennContainer: GenericContainer<SpennContainer>(System.getProperty("spenn.image", "spenn:latest"))

        private lateinit var bootstrapServers: String

        private lateinit var mockClient: MockServerClient

        @BeforeAll
        @JvmStatic
        private fun setupCluster() {
            val network = Network.newNetwork()

            val postgre = setupPostgre(network)
            postgre.start()

            val zookeeper = setupZooKeeper(network)
            zookeeper.start()

            val kafka = setupKafka(network)
            kafka.start()

            bootstrapServers = kafka.bootstrapServers

            val mq = setupMq(network)
            mq.start()

            val kafkaInit = setupTopic(network)
            val vault = setupVault(network)
            val vaultInit = setupPostgreVault(network)
            val oidcContainer = setupOidc(network)
            val mockServer = setupMockServer(network)

            kafkaInit.start()
            vault.start()
            vaultInit.start()
            oidcContainer.start()
            mockServer.start()

            val spenn = setupSpenn(network)
            spenn.start()

            mockClient = MockServerClient(mockServer.containerIpAddress, mockServer.serverPort)
        }

        private fun setupPostgre(network: Network): PostgreContainer {
            return PostgreContainer(PostgresImage)
                    .withNetwork(network)
                    .withNetworkAliases(PostgresHostname)
                    .withInitScript(PostgresInitScript)
                    .withDatabaseName(SpennDatabase)
                    .withUsername(PostgresRootUsername)
                    .withPassword(PostgresRootPassword)
        }

        private fun setupZooKeeper(network: Network): ZooKeeperContainer {
            return ZooKeeperContainer(ZooKeeperImage)
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

        private fun setupTopic(network: Network): KafkaKontainer {
            return KafkaKontainer("confluentinc/cp-kafka:5.1.0")
                    .withNetwork(network)
                    .withCommand("kafka-topics", "--create",
                            "--if-not-exists",
                            "--zookeeper",
                            "$ZooKeeperHostname:$ZooKeeperPort",
                            "--partitions",
                            "1",
                            "--replication-factor",
                            "1",
                            "--topic",
                            behovTopic)
                    .withLogConsumer { print("kafka_init: " + it.utf8String) }
        }

        private fun setupMq(network: Network): MqContainer {
            return MqContainer("ibmcom/mq:latest")
                    .withNetwork(network)
                    .withNetworkAliases(MqHostname)
                    //.withExposedPorts(MqPort)
                    .withEnv("LICENSE", "accept")
                    .withEnv("MQ_QMGR_NAME", MqQueueMananagerName)
            //.waitingFor(Wait.forListeningPort())
        }

        private fun setupVault(network: Network): VaultContainer {
            return VaultContainer(VaultImage)
                    .withNetwork(network)
                    .withNetworkAliases(VaultHostname)
                    //.withExposedPorts(VaultPort)
                    .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VaultRootToken)
            //.waitingFor(Wait.forListeningPort())
        }

        private fun setupPostgreVault(network: Network): VaultContainer {
            return VaultContainer(VaultImage)
                    .withNetwork(network)
                    .withEnv("VAULT_ADDR", "http://$VaultHostname:$VaultPort")
                    .withEnv("VAULT_TOKEN", VaultRootToken)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-setup-vault.sh"), "/setup-vault.sh")
                    .withCommand("/setup-vault.sh", "$PostgresHostname:$PostgresPort", PostgresRootUsername, PostgresRootPassword)
                    .withLogConsumer { print("vault_init: " + it.utf8String) }
                    .waitingFor(Wait.forLogMessage(".*Exit OK.*", 1))
        }

        private fun setupOidc(network: Network): OidcContainer {
            return OidcContainer("qlik/simple-oidc-provider")
                    .withNetwork(network)
                    .withNetworkAliases(OidcHostname)
                    //.withExposedPorts(9000)
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

        private fun setupSpenn(network: Network): SpennContainer {
            return SpennContainer()
                    .withNetwork(network)
                    .withEnv("VAULT_ADDR", "http://$VaultHostname:$VaultPort")
                    .withEnv("VAULT_TOKEN", VaultRootToken)
                    .withEnv("MQ_HOSTNAME", MqHostname)
                    .withEnv("MQ_PORT", "$MqPort")
                    .withEnv("SECURITYTOKENSERVICE_URL", "http://foo")
                    .withEnv("SECURITY_TOKEN_SERVICE_REST_URL", "http://$MockServerHostname:$MockServerPort")
                    .withEnv("AKTORREGISTERET_BASE_URL", "http://$MockServerHostname:$MockServerPort")
                    .withEnv("SIMULERING_SERVICE_URL", "http://$MockServerHostname:$MockServerPort")
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
                    .withEnv("SCHEDULER_ENABLED", "true")
                    .withEnv("SCHEDULER_TASKS_SIMULERING", "true")
                    .withEnv("SCHEDULER_TASKS_OPPDRAG", "true")
                    .withEnv("SCHEDULER_TASKS_AVSTEMMING", "true")
                    .withEnv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE", "audience")
                    .withEnv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL", "http://$OidcHostname:$OidcPort/.well-known/openid-configuration")
                    .waitingFor(Wait.forListeningPort())
                    .withLogConsumer { print("spenn: " + it.utf8String) }
        }
    }
}


