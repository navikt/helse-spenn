package no.nav.helse.spenn.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import no.nav.helse.spenn.RapidInspektør
import no.nav.helse.spenn.e2e.Feriepengebehov.Companion.feriepengebehov
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FeriepengerTest {
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
        Feriepenger(this, dao)
    }
    private val inspektør get() = RapidInspektør(rapid.inspektør)

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
    }

    @Test
    fun `løser feriepengebehov`() {
        val avstemmingsnøkkel = CapturingSlot<Long>()

        val behov = feriepengebehov

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
        every { dao.finnesFraFør(feriepengebehov.fnr, feriepengebehov.utbetalingId, feriepengebehov.fagsystemId) } returns false
        every { dao.oppdaterOppdrag(any(), behov.utbetalingId, FAGSYSTEMID, Oppdragstatus.OVERFØRT) } returns true

        rapid.sendTestMessage(behov.json())
        assertEquals(2, inspektør.size)
        assertMottatt(0)
        val utbetalingsmelding = rapid.inspektør.message(1)
        assertEquals("oppdrag_utbetaling", utbetalingsmelding.path("@event_name").asText())
    }

    @Test
    fun `ignorerer tomme utbetalingslinjer`() {
        val behov = feriepengebehov.linjer()
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
    fun `feriepengebehov med exception`() {
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
        rapid.sendTestMessage(feriepengebehov.json())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        inspektør.løsning(0, "Feriepengeutbetaling") {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
    }

    private fun assertMottatt(indeks: Int) {
        assertEquals(BEHOV, inspektør.behovId(indeks))
        inspektør.løsning(indeks, "Feriepengeutbetaling") {
            assertEquals(Oppdragstatus.MOTTATT.name, it.path("status").asText())
            assertDoesNotThrow { it.path("avstemmingsnøkkel").asText().toLong() }
        }
        val avstemmingsnøkkel = inspektør.løsning(indeks, "Feriepengeutbetaling")
            .path("avstemmingsnøkkel")
            .asLong()
        verify(exactly = 1) {
            dao.nyttOppdrag(
                utbetalingId = feriepengebehov.utbetalingId,
                fagområde = "SPREF",
                avstemmingsnøkkel = avstemmingsnøkkel,
                fødselsnummer = feriepengebehov.fnr,
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
