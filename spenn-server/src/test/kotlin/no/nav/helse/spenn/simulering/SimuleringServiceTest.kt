package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

internal class SimuleringServiceTest {

    @Test
    fun `simuleringsresponse med tomt ressultat skal være greit`() {
        val simulerFpServiceMock = mock(SimulerFpService::class.java)
        val transaksjonMock = mock(OppdragService.Transaksjon::class.java)
        `when`(simulerFpServiceMock.simulerBeregning(any())).thenReturn(SimulerBeregningResponse().apply {
            response = null
        })
        `when`(transaksjonMock.simuleringRequest).thenReturn(SimulerBeregningRequest())

        val simuleringService = SimuleringService(simulerFpServiceMock)
        val result = simuleringService.runSimulering(transaksjonMock)

        assertEquals(SimuleringStatus.OK, result.status)
        assertEquals(null, result.simulering)
    }
}
