package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.vault.VaultContainer
import java.net.HttpURLConnection
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@SpringBootTest(properties = [
    "spring.profiles.active=integration",
    "spring.cloud.vault.uri=http://localhost:8200",
    "spring.cloud.vault.scheme=http",
    "spring.cloud.vault.enabled=true",
    "spring.cloud.vault.authentication=TOKEN",
    "spring.cloud.vault.token=token123",
    "spring.cloud.vault.database.enabled=true",
    "spring.cloud.vault.database.role=default",
    "spring.cloud.vault.database.backend=database",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/testdb",
    "spring.test.database.replace=none",
    "KAFKA_BOOTSTRAP_SERVERS=localhost:9020",
    "SECURITY_TOKEN_SERVICE_REST_URL=localhost:8080",
    "SECURITYTOKENSERVICE_URL=localhost:8888",
    "SIMULERING_SERVICE_URL=localhost:9110",
    "STS_REST_USERNAME=foo",
    "STS_REST_PASSWORD=bar",
    "KAFKA_USERNAME=foo",
    "KAFKA_PASSWORD=bar",
    "STS_SOAP_USERNAME=foo",
    "STS_SOAP_PASSWORD=bar",
    "NAV_TRUSTSTORE_PATH=somewhere",
    "NAV_TRUSTSTORE_PASSWORD=somekey",
    "PLAIN_TEXT_KAFKA=true"])
class VaultPostgresIT {

    @Autowired
    lateinit var service: OppdragStateService

    companion object {
        var myNetwork = Network.newNetwork()
        var portBinding = Consumer<CreateContainerCmd> { e -> e.withPortBindings(PortBinding(Ports.Binding.bindPort(5432), ExposedPort(5432)))}
        var postgresContainer = PostgreSQLContainer<Nothing>().apply {
            withNetwork(myNetwork)
            withNetworkAliases("postgres")
            withCreateContainerCmdModifier(portBinding)
            withUsername("postgres")
            withPassword("postgres")
            withDatabaseName("testdb")
            waitingFor(LogMessageWaitStrategy().withRegEx(".*database system is ready to accept connections.*\\s")
                    .withTimes(2).withStartupTimeout(Duration.of(60, SECONDS)))
        }

        var vaultContainer = VaultContainer<Nothing>("vault:1.0.2").apply {
            withNetwork(myNetwork)
            withNetworkAliases("vault")
            withVaultToken("token123")
            withVaultPort(8200)
            waitingFor(HttpWaitStrategy()
                    .forPath("/v1/sys/seal-status")
                    .forStatusCode(HttpURLConnection.HTTP_OK)
            )
        }

        init{
            postgresContainer.start()
            vaultContainer.start()
            vaultContainer.execInContainer("vault", "secrets", "enable", "database")
            val url = "connection_url=postgresql://{{username}}:{{password}}@postgres:5432?sslmode=disable"
            vaultContainer.execInContainer("vault", "write", "database/config/testdb", "plugin_name=postgresql-database-plugin", "allowed_roles=default", url, "username=postgres", "password=postgres")
            vaultContainer.execInContainer("vault", "write", "database/roles/default", "db_name=testdb",
                    "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';GRANT SELECT, UPDATE, INSERT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";GRANT USAGE,  SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";",
                    "default_ttl=1h", "max_ttl=24h")
        }
      }

    @Test
    fun startVaultPostgresIntegrationTest() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")
        val state = OppdragStateDTO(soknadId = soknadKey, status = OppdragStateStatus.STARTET,
                utbetalingsOppdrag = utbetaling)
        val saved = service.saveOppdragState(state)
        assertNotNull(saved)
        val fetched = service.fetchOppdragStateByStatus(OppdragStateStatus.STARTET,100)
        assertNotNull(fetched)
        assertEquals(fetched.size,1)
    }


}
