package no.nav.helse.spenn.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.feil.FeilUnderBehandling
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

internal class SimuleringServiceTest {
    private val simulerFpService = mockk<SimulerFpService>()
    private val simuleringService = SimuleringService(simulerFpService)

    @Test
    fun `tom simuleringsrespons`() {
        every { simulerFpService.simulerBeregning(any()) } returns null
        val result = simuleringService.simulerOppdrag(SimulerBeregningRequest())
        assertEquals(SimuleringStatus.OK, result.status)
        assertNull(result.feilmelding)
        assertNull(result.simulering)
    }

    @Test
    fun `simulering med feil under behandling`() {
        every { simulerFpService.simulerBeregning(any()) } throws SimulerBeregningFeilUnderBehandling(
            "Helt feil", FeilUnderBehandling()
        )
        val result = simuleringService.simulerOppdrag(SimulerBeregningRequest())
        assertEquals(SimuleringStatus.FUNKSJONELL_FEIL, result.status)
    }

    @Test
    fun `mapping av simuleringsresponse`() {
        val forventet = SimuleringResult(
            status = SimuleringStatus.OK,
            feilmelding = null,
            simulering = Simulering(
                gjelderId = "gjelderId",
                gjelderNavn = "gjelderNavn",
                datoBeregnet = LocalDate.parse("2020-01-01"),
                totalBelop = 4500,
                periodeList = listOf(
                    SimulertPeriode(
                        fom = LocalDate.parse("2020-01-02"),
                        tom = LocalDate.parse("2020-03-02"),
                        utbetaling = listOf(
                            Utbetaling(
                                fagSystemId = "fagSystemId",
                                utbetalesTilId = "utbetalesTilId",
                                utbetalesTilNavn = "utbetalesTilNavn",
                                forfall = LocalDate.parse("2022-01-01"),
                                feilkonto = true,
                                detaljer = listOf(
                                    Detaljer(
                                        faktiskFom = LocalDate.parse("2020-01-03"),
                                        faktiskTom = LocalDate.parse("2020-03-03"),
                                        konto = "kontoStreng",
                                        belop = 3200,
                                        tilbakeforing = true,
                                        sats = 1,
                                        typeSats = "typeSats",
                                        antallSats = 2,
                                        uforegrad = 400,
                                        klassekode = "klassekode",
                                        klassekodeBeskrivelse = "klasseKodeBeskrivelse",
                                        utbetalingsType = "typeKlasse",
                                        refunderesOrgNr = "refunderesOrgNr"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse()
        val faktisk = simuleringService.simulerOppdrag(SimulerBeregningRequest())
        assertEquals(forventet, faktisk)
    }

    @Test
    fun `simuleringsrespons med desimal sats`() {
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse(
            sats = BigDecimal.valueOf(1.5)
        )
        val result = simuleringService.simulerOppdrag(SimulerBeregningRequest())
        assertEquals(2, result.simulering?.periodeList?.first()?.utbetaling?.first()?.detaljer?.first()?.sats)
    }

    @Test
    fun `simuleringsrespons med heltall som sats`() {
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse(
            sats = BigDecimal.valueOf(1)
        )
        val result = simuleringService.simulerOppdrag(SimulerBeregningRequest())
        assertEquals(1, result.simulering?.periodeList?.first()?.utbetaling?.first()?.detaljer?.first()?.sats)
    }

    @Test
    fun `simuleringsrespons med desimal beløp`() {
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse(
            beløp = BigDecimal.valueOf(500.5)
        )
        assertThrows<ArithmeticException> { simuleringService.simulerOppdrag(SimulerBeregningRequest()) }
    }

    @Test
    fun `simuleringsrespons med desimal totalbeløp`() {
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse(
            totalBeløp = BigDecimal.valueOf(500.5)
        )
        assertThrows<ArithmeticException> { simuleringService.simulerOppdrag(SimulerBeregningRequest()) }
    }

    @Test
    fun `simuleringsrespons med desimal antallSats`() {
        every { simulerFpService.simulerBeregning(any()) } returns lagSimulerBeregningResponse(
            antallSats = BigDecimal.valueOf(1.5)
        )
        assertThrows<ArithmeticException> { simuleringService.simulerOppdrag(SimulerBeregningRequest()) }
    }

    private fun lagSimulerBeregningResponse(
        totalBeløp: BigDecimal = BigDecimal.valueOf(4500),
        beløp: BigDecimal = BigDecimal.valueOf(3200),
        sats: BigDecimal = BigDecimal.valueOf(1),
        antallSats: BigDecimal = BigDecimal.valueOf(2)
    ) = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse().apply {
        response = SimulerBeregningResponse().apply {
            simulering = Beregning().apply {
                gjelderId = "gjelderId"
                gjelderNavn = "gjelderNavn"
                datoBeregnet = "2020-01-01"
                kodeFaggruppe = "kodeFaggruppe"
                belop = totalBeløp
                beregningsPeriode.add(BeregningsPeriode().apply {
                    periodeFom = "2020-01-02"
                    periodeTom = "2020-03-02"
                    beregningStoppnivaa.add(BeregningStoppnivaa().apply {
                        kodeFagomraade = "kodeFagomraade"
                        stoppNivaaId = BigInteger.valueOf(100)
                        behandlendeEnhet = "behandlendeEnhet"
                        oppdragsId = 200
                        fagsystemId = "fagSystemId"
                        kid = "kid"
                        utbetalesTilId = "utbetalesTilId"
                        utbetalesTilNavn = "utbetalesTilNavn"
                        bilagsType = "bilagsType"
                        forfall = "2022-01-01"
                        isFeilkonto = true
                        beregningStoppnivaaDetaljer.add(BeregningStoppnivaaDetaljer().apply {
                            faktiskFom = "2020-01-03"
                            faktiskTom = "2020-03-03"
                            kontoStreng = "kontoStreng"
                            behandlingskode = "behandlingskode"
                            belop = beløp
                            trekkVedtakId = 1
                            stonadId = "stonadId"
                            korrigering = "korrigering"
                            isTilbakeforing = true
                            linjeId = BigInteger.valueOf(300)
                            this.sats = sats
                            typeSats = "typeSats"
                            this.antallSats = antallSats
                            saksbehId = "saksbehId"
                            uforeGrad = BigInteger.valueOf(400)
                            kravhaverId = "kravhaverId"
                            delytelseId = "delytelseId"
                            bostedsenhet = "bostedsenhet"
                            skykldnerId = "skykldnerId"
                            klassekode = "klassekode"
                            klasseKodeBeskrivelse = "klasseKodeBeskrivelse"
                            typeKlasse = "typeKlasse"
                            typeKlasseBeskrivelse = "typeKlasseBeskrivelse"
                            refunderesOrgNr = "refunderesOrgNr"
                        })
                    })
                })
            }
        }
    }
}