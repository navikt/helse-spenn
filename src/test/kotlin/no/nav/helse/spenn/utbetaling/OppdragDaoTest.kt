package no.nav.helse.spenn.utbetaling

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
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragDaoTest {
    private companion object {
        private const val FAGOMRÅDE_REFUSJON = "SPREF"
        private const val FAGOMRÅDE_UTBETALING_BRUKER = "SP"
        private const val PERSON = "12020052345"
        private const val ORGNR = "999999999"
        private const val FAGSYSTEMID = "838069327ea2"
        private const val BELØP = Integer.MAX_VALUE
        private const val BEHOV = "{}"
        private val AVSTEMMINGSNØKKEL = System.currentTimeMillis()
        private const val SJEKKSUM = 1
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss")
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var oppdragDao: OppdragDao

    @Test
    fun `opprette oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        assertTrue(oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL,
            SJEKKSUM,
            PERSON,
            ORGNR,
            tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        ))
        finnOppdrag(AVSTEMMINGSNØKKEL).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.avstemmingsnøkkel)
            assertEquals(PERSON, it.fnr)
            assertEquals(ORGNR, it.mottaker)
            assertEquals(tidspunkt.toEpochSecond(ZoneOffset.UTC), it.opprettet.toEpochSecond(ZoneOffset.UTC))
            assertNull(it.endret)
            assertEquals(FAGSYSTEMID, it.fagsystemId)
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
        assertTrue(oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL,
            SJEKKSUM,
            PERSON,
            ORGNR,
            tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        ))
        assertFalse(oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL, SJEKKSUM + 1, "en annen person", ORGNR, tidspunkt.minusHours(1), "en annen fagsystemId", Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        ))
        assertFalse(oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL + 1, SJEKKSUM, "en annen person", ORGNR, tidspunkt.minusHours(1), "en annen fagsystemId", Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        ))
    }

    @Test
    fun `oppdatere oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        val beskrivelse = "en beskrivelse"
        val feilkode = "08"
        val melding = "original xml-melding"
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON,
            AVSTEMMINGSNØKKEL,
            SJEKKSUM,
            PERSON,
            ORGNR,
            tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        )
        assertTrue(oppdragDao.oppdaterOppdrag(
            AVSTEMMINGSNØKKEL,
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT, beskrivelse, feilkode, melding))

        finnOppdrag(AVSTEMMINGSNØKKEL).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.avstemmingsnøkkel)
            assertEquals(PERSON, it.fnr)
            assertEquals(ORGNR, it.mottaker)
            assertEquals(tidspunkt.toEpochSecond(ZoneOffset.UTC), it.opprettet.toEpochSecond(ZoneOffset.UTC))
            assertTrue(it.endret != null && it.endret > tidspunkt)
            assertEquals(FAGSYSTEMID, it.fagsystemId)
            assertEquals(Oppdragstatus.AKSEPTERT.name, it.status)
            assertEquals(BELØP, it.totalbeløp)
            assertEquals(beskrivelse, it.beskrivelse)
            assertEquals(feilkode, it.feilkode_oppdrag)
            assertEquals(melding, it.oppdrag_response)
        }
    }

    @Test
    fun `oppdrag til avstemming`() {
        val tidspunkt = LocalDateTime.now()
        val første = System.currentTimeMillis()
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første - 1,
            SJEKKSUM - 1,
            PERSON, ORGNR, tidspunkt.minusDays(1),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første,
            SJEKKSUM,
            PERSON, ORGNR, tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 1,
            SJEKKSUM + 1,
            PERSON, ORGNR, tidspunkt.plusDays(1),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 2,
            SJEKKSUM + 2,
            PERSON, ORGNR, tidspunkt.plusDays(2),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT_MED_FEIL,
            BELØP,
            BEHOV
        )
        val siste = første + 3
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, siste,
            SJEKKSUM + 3,
            PERSON, ORGNR, tidspunkt.plusDays(3),
            FAGSYSTEMID, Oppdragstatus.AVVIST,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, siste + 1,
            SJEKKSUM + 4,
            PERSON, ORGNR, tidspunkt.plusDays(4),
            FAGSYSTEMID, Oppdragstatus.AVVIST,
            BELØP,
            BEHOV
        )
        val oppdrag = oppdragDao.hentOppdragForAvstemming(første..siste)
        assertEquals(4, oppdrag.size)
        OppdragDto.avstemmingsperiode(oppdrag).also {
            assertEquals(første, it.start)
            assertEquals(siste, it.endInclusive)
        }
        OppdragDto.periode(oppdrag).also {
            assertEquals(tidspunkt.format(tidsstempel), it.start.format(
                tidsstempel
            ))
            assertEquals(tidspunkt.plusDays(3).format(tidsstempel), it.endInclusive.format(
                tidsstempel
            ))
        }
    }

    @Test
    fun `oppdrag til avstemming opp til og med`() {
        val tidspunkt = LocalDateTime.now()
        val første = System.currentTimeMillis()
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første - 1,
            SJEKKSUM - 1,
            PERSON, ORGNR, tidspunkt.minusDays(1),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første,
            SJEKKSUM,
            PERSON, ORGNR, tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 1,
            SJEKKSUM + 1,
            PERSON, ORGNR, tidspunkt.plusDays(1),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 2,
            SJEKKSUM + 2,
            PERSON, ORGNR, tidspunkt.plusDays(2),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT_MED_FEIL,
            BELØP,
            BEHOV
        )
        val siste = første + 3
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, siste,
            SJEKKSUM + 3,
            PERSON, ORGNR, tidspunkt.plusDays(3),
            FAGSYSTEMID, Oppdragstatus.AVVIST,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, siste + 1,
            SJEKKSUM + 4,
            PERSON, ORGNR, tidspunkt.plusDays(4),
            FAGSYSTEMID, Oppdragstatus.AVVIST,
            BELØP,
            BEHOV
        )
        val oppdrag = oppdragDao.hentOppdragForAvstemming(siste)
            .getValue(FAGOMRÅDE_REFUSJON)
        assertEquals(5, oppdrag.size)
        OppdragDto.avstemmingsperiode(oppdrag).also {
            assertEquals(første - 1, it.start)
            assertEquals(siste, it.endInclusive)
        }
    }

    @Test
    fun `oppdater oppdrag til avstemming`() {
        val tidspunkt = LocalDateTime.now()
        val første = System.currentTimeMillis()
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første,
            SJEKKSUM,
            PERSON, ORGNR, tidspunkt,
            FAGSYSTEMID, Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 1,
            SJEKKSUM + 1,
            PERSON, ORGNR, tidspunkt.plusDays(1),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT,
            BELØP,
            BEHOV
        )
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, første + 2,
            SJEKKSUM + 2,
            PERSON, ORGNR, tidspunkt.plusDays(2),
            FAGSYSTEMID, Oppdragstatus.AKSEPTERT_MED_FEIL,
            BELØP,
            BEHOV
        )

        assertEquals(0, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første - 1))
        assertEquals(2, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første + 1))
        assertEquals(1, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første + 3))
    }

    @Test
    fun `oppdaterer ikke andre fagområder`() {
        val tidspunkt = LocalDateTime.now()
        val avstemmingsnøkkel = System.currentTimeMillis()
        oppdragDao.nyttOppdrag(FAGOMRÅDE_REFUSJON, avstemmingsnøkkel,
            SJEKKSUM,
            PERSON,
            ORGNR,
            tidspunkt,
            FAGSYSTEMID,
            Oppdragstatus.OVERFØRT,
            BELØP,
            BEHOV
        )
        assertEquals(0, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_UTBETALING_BRUKER, avstemmingsnøkkel + 1))
    }

    private fun finnOppdrag(avstemmingsnøkkel: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT avstemmingsnokkel, fnr, mottaker, opprettet, endret, fagsystem_id, status, totalbelop, beskrivelse, feilkode_oppdrag, oppdrag_response " +
                            "FROM oppdrag " +
                            "WHERE avstemmingsnokkel = ?" +
                            "LIMIT 1",
                    avstemmingsnøkkel
                ).map {
                    TestOppdragDto(
                        avstemmingsnøkkel = it.long("avstemmingsnokkel"),
                        fnr = it.string("fnr"),
                        mottaker = it.string("mottaker"),
                        opprettet = it.localDateTime("opprettet"),
                        endret = it.localDateTimeOrNull("endret"),
                        fagsystemId = it.string("fagsystem_id"),
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

    private class TestOppdragDto(
        val avstemmingsnøkkel: Long,
        val fnr: String,
        val mottaker: String,
        val opprettet: LocalDateTime,
        val endret: LocalDateTime?,
        val fagsystemId: String,
        val status: String,
        val totalbeløp: Int,
        val beskrivelse: String?,
        val feilkode_oppdrag: String?,
        val oppdrag_response: String?
    )
}
