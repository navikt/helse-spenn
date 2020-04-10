package no.nav.helse.spenn.avstemming

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingDaoTest {
    private companion object {
        private const val FAGOMRÅDE_REFUSJON = "SPREF"
        private val ID = UUID.randomUUID()
        private val OPPRETTET = LocalDateTime.now()
        private val AVSTEMMINGSNØKKEL_TOM = System.currentTimeMillis()
        private const val ANTALL_AVSTEMTE_OPPDRAG = 1
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var avstemmingDao: AvstemmingDao

    @Test
    fun `ny avstemming`() {
        avstemmingDao.nyAvstemming(
            ID,
            FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL_TOM,
            ANTALL_AVSTEMTE_OPPDRAG
        )
        finnAvstemming().also {
            assertEquals(ID, it.id)
            assertEquals(FAGOMRÅDE_REFUSJON, it.fagområde)
            assertEquals(AVSTEMMINGSNØKKEL_TOM, it.avstemmingsnøkkelTom)
            assertEquals(ANTALL_AVSTEMTE_OPPDRAG, it.antallAvstemteOppdrag)
            assertEquals(OPPRETTET.toLocalDate(), it.opprettet.toLocalDate())
        }
    }

    private fun finnAvstemming() =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT * FROM avstemming LIMIT 1").map {
                    TestAvstemmingDto(
                        id = UUID.fromString(it.string("id")),
                        opprettet = it.localDateTime("opprettet"),
                        fagområde = it.string("fagomrade"),
                        avstemmingsnøkkelTom = it.long("avstemmingsnokkel_tom"),
                        antallAvstemteOppdrag = it.int("antall_avstemte_oppdrag")
                    )
                }.asSingle)
        } ?: fail { "Fant ikke noen avstemming" }

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        flyway = Flyway
            .configure()
            .dataSource(dataSource)
            .load()

        avstemmingDao = AvstemmingDao(dataSource)
    }

    @AfterAll
    internal fun tearDown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    private class TestAvstemmingDto(
        val id: UUID,
        val opprettet: LocalDateTime,
        val fagområde: String,
        val avstemmingsnøkkelTom: Long,
        val antallAvstemteOppdrag: Int
    )
}
