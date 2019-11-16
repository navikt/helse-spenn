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

private fun String.readResource() =
        object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: throw RuntimeException("did not find resource <$this>")

internal class SpennBlackBoxTest {

    private companion object {
        private const val behovTopic = "privat-helse-sykepenger-behov"

        private val objectMapper = ObjectMapper()

        private class PostgreContainer(dockerImageName: String) : PostgreSQLContainer<PostgreContainer>(dockerImageName) {
            override fun runInitScriptIfRequired() {
                ScriptUtils.executeDatabaseScript(databaseDelegate, "/blackbox_test-init.sql", "/blackbox_test-init.sql".readResource())
            }
        }

        private class VaultContainer(dockerImageName: String) : GenericContainer<VaultContainer>(dockerImageName)

        private class KafkaInitContainer(dockerImageName: String): GenericContainer<KafkaInitContainer>(dockerImageName)

        private class MqContainer(dockerImageName: String): GenericContainer<MqContainer>(dockerImageName)

        private class OidcContainer(dockerImageName: String): GenericContainer<OidcContainer>(dockerImageName)

        private class SpennContainer: GenericContainer<SpennContainer>(System.getProperty("spenn.image", "spenn:latest"))
    }

    private lateinit var bootstrapServers: String

    @Test
    fun `spenn svarer på et utbetalingsbehov`() {
        val mockClient = setupCluster()

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

    private fun GenericContainer<*>.hostnameAndPort() =
            "${this.getNetworkAliases()[0]}:${this.getExposedPorts()[0]}"

    private fun GenericContainer<*>.baseUrl() =
            "http://${this.hostnameAndPort()}"

    private fun setupCluster(): MockServerClient {
        val network = Network.newNetwork()

        val postgre = setupPostgre(network)
        postgre.start()

        val kafka = setupKafka(network)
        kafka.start()

        bootstrapServers = kafka.bootstrapServers

        val mq = setupMq(network)
        mq.start()

        val kafkaInit = setupTopic(network, "${kafka.networkAliases[0]}:2181", behovTopic)
        val vault = setupVault(network)
        val vaultInit = setupPostgreVault(network, vault.baseUrl(), postgre.hostnameAndPort(), postgre.username, postgre.password)
        val oidcContainer = setupOidc(network)
        val mockServer = setupMockServer(network)

        kafkaInit.start()
        vault.start()
        vaultInit.start()
        oidcContainer.start()
        mockServer.start()

        val spenn = setupSpenn(network, vault.baseUrl(), mq.networkAliases[0], mq.exposedPorts[0],
                mockServer.baseUrl(), "kafka:9092",
                "jdbc:postgresql://${postgre.hostnameAndPort()}/helse-spenn-oppdrag", oidcContainer.baseUrl())
        spenn.start()

        return MockServerClient(mockServer.containerIpAddress, mockServer.serverPort)
    }

    private fun setupPostgre(network: Network): PostgreContainer {
        return PostgreContainer("postgres:11-alpine")
                .withInitScript("blackbox_test-init.sql")
                .withDatabaseName("helse-spenn-oppdrag")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(network)
    }

    private fun setupKafka(network: Network): KafkaContainer {
        return KafkaContainer("5.1.0")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withEmbeddedZookeeper()
                .waitingFor(Wait.forListeningPort())
    }

    private fun setupTopic(network: Network, zookeeperServer: String, topic: String): KafkaInitContainer {
        return KafkaInitContainer("confluentinc/cp-kafka:5.1.0")
                .withNetwork(network)
                .withCommand("kafka-topics", "--create",
                        "--if-not-exists",
                        "--zookeeper",
                        zookeeperServer,
                        "--partitions",
                        "1",
                        "--replication-factor",
                        "1",
                        "--topic",
                        topic)
                .withLogConsumer { print("kafka_init: " + it.utf8String) }
    }

    private fun setupMq(network: Network): MqContainer {
        return MqContainer("ibmcom/mq:latest")
                .withNetwork(network)
                .withExposedPorts(1414)
                .withEnv("LICENSE", "accept")
                .withEnv("MQ_QMGR_NAME", "QM1")
                .waitingFor(Wait.forListeningPort())
    }

    private fun setupVault(network: Network): VaultContainer {
        return VaultContainer("vault:1.1.0")
                .withNetwork(network)
                .withNetworkAliases("vault")
                .withExposedPorts(8200)
                .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "token123")
                .waitingFor(Wait.forListeningPort())
    }

    private fun setupPostgreVault(network: Network, vaultUrl: String, postgresHost: String, postgresUsername: String, postgresPassword: String): VaultContainer {
        return VaultContainer("vault:1.1.0")
                .withNetwork(network)
                .withEnv("VAULT_ADDR", vaultUrl)
                .withEnv("VAULT_TOKEN", "token123")
                .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-setup-vault.sh"), "/setup-vault.sh")
                .withCommand("/setup-vault.sh", postgresHost, postgresUsername, postgresPassword)
                .withLogConsumer { print("vault_init: " + it.utf8String) }
                .waitingFor(Wait.forLogMessage(".*Exit OK.*", 1))
    }

    private fun setupOidc(network: Network): OidcContainer {
        return OidcContainer("qlik/simple-oidc-provider")
                .withNetwork(network)
                .withExposedPorts(9000)
                .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-config.json"), "/oidc/config.json")
                .withCopyFileToContainer(MountableFile.forClasspathResource("blackbox_test-users.json"), "/oidc/users.json")
                .withEnv("USERS_FILE", "/oidc/users.json")
                .withEnv("CONFIG_FILE", "/oidc/config.json")
    }

    private fun setupMockServer(network: Network): MockServerContainer {
        return MockServerContainer()
                .withNetwork(network)
                .waitingFor(Wait.forListeningPort())
    }

    private fun setupSpenn(
            network: Network,
            vaultUrl: String,
            mqHostname: String,
            mqPort: Int,
            mockServerUrl: String,
            kafkaBootstrapServer: String,
            dataSourceUrl: String,
            oidcBaseUrl: String
    ): SpennContainer {
        return SpennContainer()
                .withNetwork(network)
                .withEnv("VAULT_ADDR", vaultUrl)
                .withEnv("VAULT_TOKEN", "token123")
                .withEnv("MQ_HOSTNAME", mqHostname)
                .withEnv("MQ_PORT", "$mqPort")
                .withEnv("SECURITYTOKENSERVICE_URL", "http://foo")
                .withEnv("SECURITY_TOKEN_SERVICE_REST_URL", mockServerUrl)
                .withEnv("AKTORREGISTERET_BASE_URL", mockServerUrl)
                .withEnv("SIMULERING_SERVICE_URL", mockServerUrl)
                .withEnv("KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrapServer)
                .withEnv("KAFKA_USERNAME", "foo")
                .withEnv("KAFKA_PASSWORD", "bar")
                .withEnv("PLAIN_TEXT_KAFKA", "true")
                .withEnv("NAV_TRUSTSTORE_PATH", "foo")
                .withEnv("NAV_TRUSTSTORE_PASSWORD", "bar")
                .withEnv("STS_SOAP_USERNAME", "foo")
                .withEnv("STS_SOAP_PASSWORD", "bar")
                .withEnv("DATASOURCE_URL", dataSourceUrl)
                .withEnv("DATASOURCE_VAULT_ENABLED", "true")
                .withEnv("SCHEDULER_ENABLED", "true")
                .withEnv("SCHEDULER_TASKS_SIMULERING", "true")
                .withEnv("SCHEDULER_TASKS_OPPDRAG", "true")
                .withEnv("SCHEDULER_TASKS_AVSTEMMING", "true")
                .withEnv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE", "audience")
                .withEnv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL", "${oidcBaseUrl}/.well-known/openid-configuration")
                .waitingFor(Wait.forListeningPort())
                .withLogConsumer { print("spenn: " + it.utf8String) }
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
}


