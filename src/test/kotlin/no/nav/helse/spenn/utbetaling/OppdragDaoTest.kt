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
        assertTrue(oppdragDao.nyttOppdrag(
            FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
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
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertFalse(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = "en annen person",
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.minusHours(1),
            fagsystemId = "en annen fagsystemId",
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertFalse(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL + 1,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.minusHours(1),
            fagsystemId = "en annen fagsystemId",
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
    }

    @Test
    fun `oppdatere oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        val beskrivelse = "en beskrivelse"
        val feilkode = "08"
        val melding = "original xml-melding"
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
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
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første - 1,
            sjekksum = SJEKKSUM - 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.minusDays(1),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 1,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(1),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 2,
            sjekksum = SJEKKSUM + 2,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(2),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT_MED_FEIL,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        val siste = første + 3
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = siste,
            sjekksum = SJEKKSUM + 3,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(3),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AVVIST,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = siste + 1,
            sjekksum = SJEKKSUM + 4,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(4),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AVVIST,
            totalbeløp = BELØP,
            originalJson = BEHOV
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
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første - 1,
            sjekksum = SJEKKSUM - 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.minusDays(1),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 1,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(1),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 2,
            sjekksum = SJEKKSUM + 2,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(2),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT_MED_FEIL,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        val siste = første + 3
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = siste,
            sjekksum = SJEKKSUM + 3,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(3),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AVVIST,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = siste + 1,
            sjekksum = SJEKKSUM + 4,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(4),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AVVIST,
            totalbeløp = BELØP,
            originalJson = BEHOV
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
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 1,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(1),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = første + 2,
            sjekksum = SJEKKSUM + 2,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt.plusDays(2),
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.AKSEPTERT_MED_FEIL,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )

        assertEquals(0, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første - 1))
        assertEquals(2, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første + 1))
        assertEquals(1, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_REFUSJON, første + 3))
    }

    @Test
    fun `oppdaterer ikke andre fagområder`() {
        val tidspunkt = LocalDateTime.now()
        val avstemmingsnøkkel = System.currentTimeMillis()
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        )
        assertEquals(0, oppdragDao.oppdaterAvstemteOppdrag(FAGOMRÅDE_UTBETALING_BRUKER, avstemmingsnøkkel + 1))
    }

    @Test
    fun `Insert feiler dersom constraint sjekksum er lik`() {
        val tidspunkt = LocalDateTime.now()
        val avstemmingsnøkkel = System.currentTimeMillis()
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertFalse(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel + 1,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel + 1,
            sjekksum = SJEKKSUM,
            fødselsnummer = "en annen person",
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
    }

    @Test
    fun `Insert er vellykket for ulike kombinasjoner av constraint sjekksum`() {
        val tidspunkt = LocalDateTime.now()
        val avstemmingsnøkkel = System.currentTimeMillis()
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel,
            sjekksum = SJEKKSUM,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel + 1,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = "annenPerson",
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel + 2,
            sjekksum = SJEKKSUM + 2,
            fødselsnummer = PERSON,
            organisasjonsnummer = "annenOrg",
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
        assertTrue(oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = avstemmingsnøkkel + 3,
            sjekksum = SJEKKSUM + 1,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV
        ))
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
