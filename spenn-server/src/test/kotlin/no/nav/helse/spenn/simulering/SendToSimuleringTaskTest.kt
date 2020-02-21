package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SendToSimuleringTaskTest {

    private val mockPersistence = mockk<OppdragService>()
    private val mockSimuleringService = mockk<SimuleringService>()
    private val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    @Test
    fun sendToSimulering() {
        val sendToSimuleringTask = SendToSimuleringTask(
            simuleringService = mockSimuleringService,
            meterRegistry = mockMeterRegistry,
            oppdragService = mockPersistence
        )

        val trans1 = mockk<OppdragService.Transaksjon>().apply {
            every { oppdaterSimuleringsresultat(any()) } returns Unit
        }
        val trans2 = mockk<OppdragService.Transaksjon>().apply {
            every { oppdaterSimuleringsresultat(any()) } returns Unit
        }

        every { mockPersistence.hentNyeOppdrag(100) } returns listOf(trans1, trans2)
        every { mockSimuleringService.runSimulering(trans1) } returns simulertOppdragEn
        every { mockSimuleringService.runSimulering(trans2) } returns simulertOppdragTo

        sendToSimuleringTask.sendSimulering()

        verify(exactly = 1) { trans1.oppdaterSimuleringsresultat(simulertOppdragEn) }
        verify(exactly = 1) { trans2.oppdaterSimuleringsresultat(simulertOppdragTo) }
    }
}

val simuleringsResultat = SimuleringResult(
    status = SimuleringStatus.OK,
    simulering = Simulering(
        gjelderId = "", gjelderNavn = "", datoBeregnet = LocalDate.now(),
        totalBelop = BigDecimal.TEN, periodeList = emptyList()
    ), feilMelding = ""
)
val simulertOppdragEn =
    simuleringsResultat.copy(simulering = simuleringsResultat.simulering!!.copy(gjelderNavn = "Førstemann"))
val simulertOppdragTo =
    simuleringsResultat.copy(simulering = simuleringsResultat.simulering!!.copy(gjelderNavn = "Andremann"))
