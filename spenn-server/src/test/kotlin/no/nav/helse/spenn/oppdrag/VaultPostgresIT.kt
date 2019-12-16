package no.nav.helse.spenn.oppdrag
/*
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.vault.VaultContainer
import java.net.HttpURLConnection
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VaultPostgresIT {
    private lateinit var service: OppdragService

    private var myNetwork = Network.newNetwork()
    private var portBinding = Consumer<CreateContainerCmd> { e -> e.withPortBindings(PortBinding(Ports.Binding.bindPort(5432), ExposedPort(5432)))}
    private var postgresContainer = PostgreSQLContainer<Nothing>().apply {
        withNetwork(myNetwork)
        withNetworkAliases("postgres")
        withCreateContainerCmdModifier(portBinding)
        withUsername("postgres")
        withPassword("postgres")
        withDatabaseName("testdb")
        waitingFor(LogMessageWaitStrategy().withRegEx(".*database system is ready to accept connections.*\\s")
                .withTimes(2).withStartupTimeout(Duration.of(60, SECONDS)))
    }

    private var vaultContainer = VaultContainer<Nothing>("vault:1.0.2").apply {
        withNetwork(myNetwork)
        withNetworkAliases("vault")
        withVaultToken("token123")
        withVaultPort(8200)
        waitingFor(HttpWaitStrategy()
                .forPath("/v1/sys/seal-status")
                .forStatusCode(HttpURLConnection.HTTP_OK)
        )
    }

    init {
        postgresContainer.start()
        vaultContainer.start()
        vaultContainer.execInContainer("vault", "secrets", "enable", "database")
        val url = "connection_url=postgresql://{{username}}:{{password}}@postgres:5432?sslmode=disable"
        vaultContainer.execInContainer("vault", "write", "database/config/testdb", "plugin_name=postgresql-database-plugin", "allowed_roles=default", url, "username=postgres", "password=postgres")
        vaultContainer.execInContainer("vault", "write", "database/roles/default", "db_name=testdb",
                "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';GRANT SELECT, UPDATE, INSERT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";GRANT USAGE,  SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";",
                "default_ttl=1h", "max_ttl=24h")
    }

    @Test
    fun startVaultPostgresIntegrationTest() {
        val behov = etEnkeltBehov()
        val utbetaling = behov.tilUtbetaling("12345678901")
        val saved = service.lagreNyttOppdrag(utbetaling)
        assertNotNull(saved)
        val fetched = service.hentNyeOppdrag(5)
        assertNotNull(fetched)
        assertEquals(fetched.size,1)
    }


}


 */