package no.nav.helse.spenn.avstemming

import com.github.navikt.tbd_libs.test_support.TestDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spenn.avstemming.e2e.E2ETest.RepublishableTestRapid
import no.nav.helse.spenn.avstemming.e2e.E2ETest.TestDatabase
import no.nav.helse.spenn.avstemming.e2e.databaseContainer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Path
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

    private lateinit var dataSource: TestDataSource
    private lateinit var avstemmingDao: AvstemmingDao

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        avstemmingDao = AvstemmingDao { dataSource.ds }
    }

    @AfterEach
    fun teardown() {
        // gi tilbake tilkoblingen
        databaseContainer.droppTilkobling(dataSource)
    }

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
        sessionOf(dataSource.ds).use { session ->
            session.run(
                queryOf("SELECT * FROM avstemming LIMIT 1").map {
                    TestAvstemmingDto(
                        id = UUID.fromString(it.string("id")),
                        opprettet = it.localDateTime("opprettet"),
                        fagområde = it.string("fagomrade"),
                        avstemmingsnøkkelTom = it.long("avstemmingsnokkel_tom"),
                        antallAvstemteOppdrag = it.int("antall_avstemte_oppdrag")
                    )
                }.asSingle
            )
        } ?: fail { "Fant ikke noen avstemming" }

    private class TestAvstemmingDto(
        val id: UUID,
        val opprettet: LocalDateTime,
        val fagområde: String,
        val avstemmingsnøkkelTom: Long,
        val antallAvstemteOppdrag: Int
    )
}
