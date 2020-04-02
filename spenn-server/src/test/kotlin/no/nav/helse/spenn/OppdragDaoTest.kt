package no.nav.helse.spenn

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.postgresql.util.PSQLException
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragDaoTest {
    private companion object {
        private const val PERSON = "12020052345"
        private const val UTBETALINGSREF = "838069327ea2"
        private const val BELØP = Integer.MAX_VALUE
        private val AVSTEMMINGSNØKKEL = System.currentTimeMillis()
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var oppdragDao: OppdragDao

    @Test
    fun `opprette oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        assertTrue(oppdragDao.nyttOppdrag(AVSTEMMINGSNØKKEL, PERSON, tidspunkt, UTBETALINGSREF, Oppdragstatus.OVERFØRT, BELØP))
        finnOppdrag(AVSTEMMINGSNØKKEL).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.avstemmingsnøkkel)
            assertEquals(PERSON, it.fnr)
            assertEquals(tidspunkt.toEpochSecond(ZoneOffset.UTC), it.opprettet.toEpochSecond(ZoneOffset.UTC))
            assertNull(it.endret)
            assertEquals(UTBETALINGSREF, it.utbetalingsreferanse)
            assertEquals(Oppdragstatus.OVERFØRT.name, it.status)
            assertEquals(BELØP, it.totalbeløp)
            assertNull(it.beskrivelse)
            assertNull(it.feilkode_oppdrag)
            assertNull(it.oppdrag_response)
        }
    }

    @Test
    fun `duplikat oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        assertTrue(oppdragDao.nyttOppdrag(AVSTEMMINGSNØKKEL, PERSON, tidspunkt, UTBETALINGSREF, Oppdragstatus.OVERFØRT, BELØP))
        assertThrows<PSQLException> { oppdragDao.nyttOppdrag(AVSTEMMINGSNØKKEL, "en annen person", tidspunkt.minusHours(1), "en annen utbetalingsreferanse", Oppdragstatus.AKSEPTERT, BELØP) }
    }

    @Test
    fun `oppdatere oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        val beskrivelse = "en beskrivelse"
        val feilkode = "08"
        val melding = "original xml-melding"
        oppdragDao.nyttOppdrag(AVSTEMMINGSNØKKEL, PERSON, tidspunkt, UTBETALINGSREF, Oppdragstatus.OVERFØRT, BELØP)
        assertTrue(oppdragDao.oppdaterOppdrag(AVSTEMMINGSNØKKEL, UTBETALINGSREF, Oppdragstatus.AKSEPTERT, beskrivelse, feilkode, melding))

        finnOppdrag(AVSTEMMINGSNØKKEL).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.avstemmingsnøkkel)
            assertEquals(PERSON, it.fnr)
            assertEquals(tidspunkt.toEpochSecond(ZoneOffset.UTC), it.opprettet.toEpochSecond(ZoneOffset.UTC))
            assertTrue(it.endret != null && it.endret > tidspunkt)
            assertEquals(UTBETALINGSREF, it.utbetalingsreferanse)
            assertEquals(Oppdragstatus.AKSEPTERT.name, it.status)
            assertEquals(BELØP, it.totalbeløp)
            assertEquals(beskrivelse, it.beskrivelse)
            assertEquals(feilkode, it.feilkode_oppdrag)
            assertEquals(melding, it.oppdrag_response)
        }
    }

    private fun finnOppdrag(avstemmingsnøkkel: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT avstemmingsnokkel, fnr, opprettet, endret, utbetalingsreferanse, status, totalbelop, beskrivelse, feilkode_oppdrag, oppdrag_response " +
                            "FROM oppdrag " +
                            "WHERE avstemmingsnokkel = ?" +
                            "LIMIT 1",
                    avstemmingsnøkkel
                ).map {
                    OppdragDto(
                        avstemmingsnøkkel = it.long("avstemmingsnokkel"),
                        fnr = it.string("fnr"),
                        opprettet = it.localDateTime("opprettet"),
                        endret = it.localDateTimeOrNull("endret"),
                        utbetalingsreferanse = it.string("utbetalingsreferanse"),
                        status = it.string("status"),
                        totalbeløp = it.int("totalbelop"),
                        beskrivelse = it.stringOrNull("beskrivelse"),
                        feilkode_oppdrag = it.stringOrNull("feilkode_oppdrag"),
                        oppdrag_response = it.stringOrNull("oppdrag_response")
                    )
                }.asSingle)
        } ?: fail { "Fant ikke oppdrag med avstemmingsnøkkel $avstemmingsnøkkel" }

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

        oppdragDao = OppdragDao(dataSource)
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

    private class OppdragDto(
        val avstemmingsnøkkel: Long,
        val fnr: String,
        val opprettet: LocalDateTime,
        val endret: LocalDateTime?,
        val utbetalingsreferanse: String,
        val status: String,
        val totalbeløp: Int,
        val beskrivelse: String?,
        val feilkode_oppdrag: String?,
        val oppdrag_response: String?
    )
}
