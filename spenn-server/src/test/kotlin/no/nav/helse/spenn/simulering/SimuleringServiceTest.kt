package no.nav.helse.spenn.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SimuleringServiceTest {

    @Test
    fun `simuleringsresponse med tomt ressultat skal være greit`() {
        val simulerFpServiceMock = mockk<SimulerFpService>()
        val transaksjonMock = mockk<OppdragService.Transaksjon>()
        every { simulerFpServiceMock.simulerBeregning(any()) } returns SimulerBeregningResponse().apply {
            response = null
        }
        every { transaksjonMock.simuleringRequest } returns SimulerBeregningRequest()

        val simuleringService = SimuleringService(simulerFpServiceMock)
        val result = simuleringService.runSimulering(transaksjonMock)

        assertEquals(SimuleringStatus.OK, result.status)
        assertEquals(null, result.simulering)
    }
}
