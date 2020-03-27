package no.nav.helse.spenn

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.simulering.Simulering
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.SimuleringStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertTrue

internal class SimuleringløserTest {

    private companion object {
        private val PERSON = UUID.randomUUID().toString()
        private val ORGNR = "123456789"
        private val BEHOV = UUID.randomUUID().toString()
    }

    private lateinit var resultat: SimuleringResult
    private val simuleringService = mockk<SimuleringService>()
    init {
        every {
            simuleringService.simulerOppdrag(any())
        } answers  {
            resultat
        }
    }

    private val rapid = TestRapid().apply {
        Simuleringløser(this, simuleringService)
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
        val melding = rapid.inspektør.melding(0)
        assertEquals(BEHOV, melding["@id"].asText())
        assertEquals("OK", melding.path("@løsning").path("Simulering").path("status").asText())
        assertFalse(melding.path("@løsning").path("Simulering").path("simulering").isNull)
    }

    @Test
    fun `løser simuleringsbehov med simuleringfeil`() {
        resultat(SimuleringStatus.FEIL)
        rapid.sendTestMessage(simuleringbehov())

        assertEquals(1, rapid.inspektør.antall())
        val melding = rapid.inspektør.melding(0)
        assertEquals(BEHOV, melding["@id"].asText())
        assertEquals("FEIL", melding.path("@løsning").path("Simulering").path("status").asText())
        assertTrue(melding.path("@løsning").path("Simulering").path("simulering").isNull)
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

    private fun simuleringbehov(): String {
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
                "utbetalingslinjer" to listOf(
                    mapOf(
                        "dagsats" to "1000.0",
                        "fom" to "2020-04-20",
                        "tom" to "2020-05-20",
                        "grad" to 100
                    )
                )
            )
        )
    }
}
