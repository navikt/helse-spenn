package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.RapidInspektør
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.tjenester.simulerfpservice.feil.FeilUnderBehandling
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

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
    private val inspektør get() = RapidInspektør(rapid.inspektør)

    @BeforeEach
    fun clear() {
        rapid.reset()
    }

    @Test
    fun `løser simuleringsbehov`() {
        resultat(SimuleringStatus.OK)
        rapid.sendTestMessage(simuleringbehov())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        assertEquals("OK", inspektør.løsning(0, "Simulering").path("status").asText())
        assertFalse(inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    @Test
    fun `løser simuleringsbehov for engangsutbetaling`() {
        resultat(SimuleringStatus.OK)
        val utbetalingslinjer = listOf(
            mapOf(
                "satstype" to "ENG",
                "sats" to 10000,
                "fom" to "2020-04-20",
                "tom" to "2020-04-20"
            )
        )
        rapid.sendTestMessage(simuleringbehov(utbetalingslinjer))
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        assertEquals("OK", inspektør.løsning(0, "Simulering").path("status").asText())
        assertFalse(inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    @Test
    fun `ignorerer simuleringsbehov med tomme utbetalingslinjer`() {
        rapid.sendTestMessage(simuleringbehov(emptyList()))
        assertEquals(0, inspektør.size)
    }

    @Test
    fun `løser simuleringsbehov med simuleringfeil`() {
        resultat(SimuleringStatus.FUNKSJONELL_FEIL)
        rapid.sendTestMessage(simuleringbehov())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        assertEquals("FUNKSJONELL_FEIL", inspektør.løsning(0, "Simulering").path("status").asText())
        assertTrue(inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    @Test
    fun `løser simuleringsbehov med teknisk feil`() {
        resultat(SimuleringStatus.TEKNISK_FEIL)
        rapid.sendTestMessage(simuleringbehov())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        assertEquals("TEKNISK_FEIL", inspektør.løsning(0, "Simulering").path("status").asText())
        assertTrue(inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }

    @Test
    fun `løser simuleringsbehov for utbetaling til bruker`() {
        resultat(SimuleringStatus.OK)
        rapid.sendTestMessage(simuleringbehovBruker())
        assertEquals(1, inspektør.size)
        assertEquals(BEHOV, inspektør.behovId(0))
        assertEquals("OK", inspektør.løsning(0, "Simulering").path("status").asText())
        assertFalse(inspektør.løsning(0, "Simulering").path("simulering").isNull)
    }


    private fun resultat(status: SimuleringStatus) = SimuleringResult(
        status = status,
        feilmelding = if (status != SimuleringStatus.OK) "Error message" else "",
        simulering = if (status == SimuleringStatus.OK) Simulering(
            gjelderId = PERSON,
            gjelderNavn = "Navn Navnesen",
            datoBeregnet = LocalDate.now(),
            totalBelop = 1000,
            periodeList = emptyList()
        ) else null
    ).also {
        resultat = it
    }

    private fun simuleringbehovBruker(
        utbetalingslinjer: List<Map<String, Any>> = listOf(
            mapOf(
                "satstype" to "DAG",
                "sats" to 1000,
                "fom" to "2020-04-20",
                "tom" to "2020-05-20",
                "grad" to 100
            )
        )
    ): String {
        return jacksonObjectMapper().writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Simulering"),
                "@id" to UUID.randomUUID(),
                "@behovId" to BEHOV,
                "organisasjonsnummer" to ORGNR,
                "fødselsnummer" to PERSON,
                "Simulering" to mapOf(
                    "mottaker" to PERSON,
                    "maksdato" to "2020-04-20",
                    "saksbehandler" to "Spleis",
                    "fagområde" to "SP",
                    "fagsystemId" to "ref",
                    "endringskode" to "NY",
                    "sjekksum" to -873852214,
                    "linjer" to utbetalingslinjer.map {
                        mapOf(
                            "fom" to it["fom"],
                            "tom" to it["tom"],
                            "sats" to it["sats"],
                            "satstype" to it["satstype"],
                            "grad" to it["grad"],
                            "delytelseId" to 1,
                            "refDelytelseId" to null,
                            "refFagsystemId" to null,
                            "endringskode" to "NY",
                            "klassekode" to "SPATORD",
                            "datoStatusFom" to null,
                            "statuskode" to null
                        )
                    }
                )
            )
        )
    }

    private fun simuleringbehov(
        utbetalingslinjer: List<Map<String, Any>> = listOf(
            mapOf(
                "satstype" to "DAG",
                "sats" to 1000,
                "fom" to "2020-04-20",
                "tom" to "2020-05-20",
                "grad" to 100
            )
        )
    ): String {
        return jacksonObjectMapper().writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Simulering"),
                "@id" to UUID.randomUUID(),
                "@behovId" to BEHOV,
                "organisasjonsnummer" to ORGNR,
                "fødselsnummer" to PERSON,
                "Simulering" to mapOf(
                    "mottaker" to ORGNR,
                    "maksdato" to "2020-04-20",
                    "saksbehandler" to "Spleis",
                    "mottaker" to ORGNR,
                    "fagområde" to "SPREF",
                    "fagsystemId" to "ref",
                    "endringskode" to "NY",
                    "sjekksum" to -873852214,
                    "linjer" to utbetalingslinjer.map {
                        mapOf<String, Any?>(
                            "fom" to it["fom"],
                            "tom" to it["tom"],
                            "sats" to it["sats"],
                            "satstype" to it["satstype"],
                            "grad" to it["grad"],
                            "delytelseId" to 1,
                            "refDelytelseId" to null,
                            "refFagsystemId" to null,
                            "endringskode" to "NY",
                            "klassekode" to "SPREFAG-IOP",
                            "datoStatusFom" to null,
                            "statuskode" to null
                        )
                    }
                )
            )
        )
    }
}
