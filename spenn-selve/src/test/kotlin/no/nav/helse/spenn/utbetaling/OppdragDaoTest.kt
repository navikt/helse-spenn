package no.nav.helse.spenn.utbetaling

import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spenn.e2e.databaseContainer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

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
        private val UTBETALING_ID = UUID.randomUUID()
    }

    private lateinit var dataSource: TestDataSource
    private lateinit var oppdragDao: OppdragDao

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        oppdragDao = OppdragDao(dataSource.ds)
    }

    @AfterEach
    fun teardown() {
        // gi tilbake tilkoblingen
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `opprette oppdrag`() {
        val tidspunkt = LocalDateTime.now()
        assertNotNull(
            oppdragDao.nyttOppdrag(
                FAGOMRÅDE_REFUSJON,
                avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
                fødselsnummer = PERSON,
                organisasjonsnummer = ORGNR,
                utbetalingId = UTBETALING_ID,
                mottaker = ORGNR,
                tidspunkt = tidspunkt,
                fagsystemId = FAGSYSTEMID,
                status = Oppdragstatus.OVERFØRT,
                totalbeløp = BELØP,
                originalJson = BEHOV,
            )
        )
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
    fun `oppdatere oppdrag`() {
        val tidspunkt = LocalDateTime.now().minusHours(4)
        val beskrivelse = "en beskrivelse"
        val feilkode = "08"
        val melding = "original xml-melding"
        oppdragDao.nyttOppdrag(
            fagområde = FAGOMRÅDE_REFUSJON,
            avstemmingsnøkkel = AVSTEMMINGSNØKKEL,
            fødselsnummer = PERSON,
            organisasjonsnummer = ORGNR,
            utbetalingId = UTBETALING_ID,
            mottaker = ORGNR,
            tidspunkt = tidspunkt,
            fagsystemId = FAGSYSTEMID,
            status = Oppdragstatus.OVERFØRT,
            totalbeløp = BELØP,
            originalJson = BEHOV,
        )
        assertTrue(oppdragDao.oppdaterOppdrag(UTBETALING_ID, FAGSYSTEMID, Oppdragstatus.AKSEPTERT, beskrivelse, feilkode, melding))

        finnOppdrag(AVSTEMMINGSNØKKEL).also {
            assertEquals(AVSTEMMINGSNØKKEL, it.avstemmingsnøkkel)
            assertEquals(PERSON, it.fnr)
            assertEquals(ORGNR, it.mottaker)
            assertEquals(tidspunkt.toEpochSecond(ZoneOffset.UTC), it.opprettet.toEpochSecond(ZoneOffset.UTC))
            assertTrue(it.endret != null && it.endret > tidspunkt) { "${it.endret} er ikke større enn $tidspunkt" }
            assertEquals(FAGSYSTEMID, it.fagsystemId)
            assertEquals(Oppdragstatus.AKSEPTERT.name, it.status)
            assertEquals(BELØP, it.totalbeløp)
            assertEquals(beskrivelse, it.beskrivelse)
            assertEquals(feilkode, it.feilkode_oppdrag)
            assertEquals(melding, it.oppdrag_response)
        }
    }

    private fun finnOppdrag(avstemmingsnøkkel: Long) =
        sessionOf(dataSource.ds).use { session ->
            @Language("PostgreSQL")
            val stmt = """
                |SELECT avstemmingsnokkel, fnr, mottaker, opprettet, endret, fagsystem_id, status, totalbelop, beskrivelse, feilkode_oppdrag, oppdrag_response 
                |FROM oppdrag
                |WHERE avstemmingsnokkel = ?
                |LIMIT 1
            """.trimMargin()
            session.run(
                queryOf(stmt, avstemmingsnøkkel).map {
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
                }.asSingle
            )
        } ?: fail { "Fant ikke oppdrag med avstemmingsnøkkel $avstemmingsnøkkel" }

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
