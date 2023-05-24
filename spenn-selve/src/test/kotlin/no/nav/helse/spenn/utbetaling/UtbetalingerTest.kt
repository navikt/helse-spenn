package no.nav.helse.spenn.utbetaling

import io.mockk.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.RapidInspektør
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.e2e.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingerTest {
    companion object {
        const val PERSON = "12345678911"
        const val ORGNR = "123456789"
        const val BELØP = 1000
        const val FAGSYSTEMID = "838069327ea2"
        const val BEHOV = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        const val SAKSBEHANDLER = "Navn Navnesen"
    }

    private val dao = mockk<OppdragDao>()
    private val rapid = TestRapid().apply {
        Utbetalinger(this, dao)
    }
    private val inspektør get() = RapidInspektør(rapid.inspektør)

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
    }

    @Test
    fun `løser utbetalingsbehov`() {
        val avstemmingsnøkkel = CapturingSlot<Long>()
        val behov = utbetalingsbehov
        every {
            dao.hentOppdrag(any(), any(), any())
        } returns null
        every {
            dao.nyttOppdrag(
                any(),
                any(),
                capture(avstemmingsnøkkel),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            OppdragDto(
                avstemmingsnøkkel.captured,
                LocalDateTime.now(),
                Oppdragstatus.MOTTATT
            )
        }
        every { dao.oppdaterOppdrag(any(), behov.utbetalingId, behov.fagsystemId, Oppdragstatus.OVERFØRT) } returns true
        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId, behov.fagsystemId) } returns false
        rapid.sendTestMessage(behov.json())
        assertEquals(2, inspektør.size)
        assertMottatt(0)
        val utbetalingsmelding = rapid.inspektør.message(1)
        assertEquals("oppdrag_utbetaling", utbetalingsmelding.path("@event_name").asText())
    }

    @Test
    fun `løser utbetalingsbehov med engangsutbetaling`() {
        val avstemmingsnøkkel = CapturingSlot<Long>()

        val behov = utbetalingsbehov.linjer(
            utbetalingslinje
                .grad(null)
                .satstype("ENG")
        )

        every {
            dao.hentOppdrag(any(), any(), any())
        } returns null

        every {
            dao.nyttOppdrag(
                any(),
                any(),
                capture(avstemmingsnøkkel),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            OppdragDto(
                avstemmingsnøkkel.captured,
                LocalDateTime.now(),
                Oppdragstatus.MOTTATT
            )
        }
        every { dao.finnesFraFør(utbetalingsbehov.fnr, utbetalingsbehov.utbetalingId, utbetalingsbehov.fagsystemId) } returns false
        every { dao.oppdaterOppdrag(any(), behov.utbetalingId, FAGSYSTEMID, Oppdragstatus.OVERFØRT) } returns true

        rapid.sendTestMessage(behov.json())
        assertEquals(2, inspektør.size)
        assertMottatt(0)
        val utbetalingsmelding = rapid.inspektør.message(1)
        assertEquals("oppdrag_utbetaling", utbetalingsmelding.path("@event_name").asText())
    }


    @Test
    fun `ignorerer tomme utbetalingslinjer`() {
        val behov = utbetalingsbehov.linjer()
        rapid.sendTestMessage(behov.json())
        assertEquals(0, inspektør.size)
        verify(exactly = 0) {
            dao.nyttOppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `utbetalingsbehov med exception`() {
        every {
            dao.nyttOppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException()
        rapid.sendTestMessage(utbetalingsbehov.json())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        inspektør.løsning(0, "Utbetaling") {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
    }

    private fun assertMottatt(indeks: Int) {
        assertEquals(BEHOV, inspektør.behovId(indeks))
        inspektør.løsning(indeks, "Utbetaling") {
            assertEquals(Oppdragstatus.MOTTATT.name, it.path("status").asText())
            assertDoesNotThrow { it.path("avstemmingsnøkkel").asText().toLong() }
        }
        val avstemmingsnøkkel = inspektør.løsning(indeks, "Utbetaling")
            .path("avstemmingsnøkkel")
            .asLong()
        verify(exactly = 1) {
            dao.nyttOppdrag(
                utbetalingId = utbetalingsbehov.utbetalingId,
                fagområde = "SPREF",
                avstemmingsnøkkel = avstemmingsnøkkel,
                fødselsnummer = utbetalingsbehov.fnr,
                organisasjonsnummer = ORGNR,
                mottaker = ORGNR,
                tidspunkt = any(),
                fagsystemId = FAGSYSTEMID,
                status = Oppdragstatus.MOTTATT,
                totalbeløp = BELØP,
                originalJson = any()
            )
        }
    }
}
