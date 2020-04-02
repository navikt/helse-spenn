package no.nav.helse.spenn

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.simulering.Simulering
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.SimuleringStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

internal class SimuleringerTest {

    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val BEHOV = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
    }

    private lateinit var resultat: SimuleringResult
    private val simuleringService = mockk<SimuleringService>()
    init {
        every {
            simuleringService.simulerOppdrag(any())
        } answers {
            resultat
        }
    }

    private val rapid = TestRapid().apply {
        Simuleringer(this, simuleringService)
    }

    @BeforeEach
    fun clear() {
        rapid.reset()
    }

    @Test
    fun `løser simuleringsbehov`() {
        resultat(SimuleringStatus.OK)
        rapid.sendTestMessage(simuleringbehov())
        assertEquals(1, rapid.inspektør.antall())
        assertEquals(BEHOV, rapid.inspektør.id(0))
        assertEquals("OK", rapid.inspektør.løsning(0, "Simulering").path("status").asText())
        assertFalse(rapid.inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    @Test
    fun `ignorerer simuleringsbehov med tomme utbetalingslinjer`() {
        rapid.sendTestMessage(simuleringbehov(emptyList()))
        assertEquals(0, rapid.inspektør.antall())
    }

    @Test
    fun `løser simuleringsbehov med simuleringfeil`() {
        resultat(SimuleringStatus.FEIL)
        rapid.sendTestMessage(simuleringbehov())
        assertEquals(1, rapid.inspektør.antall())
        assertEquals(BEHOV, rapid.inspektør.id(0))
        assertEquals("FEIL", rapid.inspektør.løsning(0, "Simulering").path("status").asText())
        assertTrue(rapid.inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    private fun resultat(status: SimuleringStatus) = SimuleringResult(
        status = status,
        feilMelding = if (status != SimuleringStatus.OK) "Error message" else "",
        simulering = if (status == SimuleringStatus.OK) Simulering(
            gjelderId = PERSON,
            gjelderNavn = "Navn Navnesen",
            datoBeregnet = LocalDate.now(),
            totalBelop = 1000.toBigDecimal(),
            periodeList = emptyList()
        ) else null
    ).also {
        resultat = it
    }

    private fun simuleringbehov(utbetalingslinjer: List<Map<String, Any>> = listOf(
        mapOf(
            "dagsats" to "1000.0",
            "fom" to "2020-04-20",
            "tom" to "2020-05-20",
            "grad" to 100
        )
    )): String {
        return defaultObjectMapper.writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Simulering"),
                "@id" to BEHOV,
                "organisasjonsnummer" to ORGNR,
                "fødselsnummer" to PERSON,
                "maksdato" to "2020-04-20",
                "utbetalingsreferanse" to "ref",
                "forlengelse" to false,
                "utbetalingslinjer" to utbetalingslinjer
            )
        )
    }
}
