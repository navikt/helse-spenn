package no.nav.helse.spenn.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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
}