package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.LocalDate

class SendToSimuleringTaskTest {

    val mockPersistence = mock(OppdragService::class.java)
    val mockSimuleringService = mock(SimuleringService::class.java)
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    @Test
    fun sendToSimulering() {
        val sendToSimuleringTask = SendToSimuleringTask(simuleringService = mockSimuleringService,
                meterRegistry = mockMeterRegistry, oppdragService = mockPersistence)

        val trans1 = mock(OppdragService.Transaksjon::class.java)
        val trans2 = mock(OppdragService.Transaksjon::class.java)

        `when`(mockPersistence.hentNyeOppdrag(100)).thenReturn(listOf(trans1, trans2))
        `when`(mockSimuleringService.runSimulering(trans1)).thenReturn(simulertOppdragEn)
        `when`(mockSimuleringService.runSimulering(trans2)).thenReturn(simulertOppdragTo)

        sendToSimuleringTask.sendSimulering()

        verify(trans1, times(1)).oppdaterSimuleringsresultat(simulertOppdragEn)
        verify(trans2, times(1)).oppdaterSimuleringsresultat(simulertOppdragTo)
    }
}

val simuleringsResultat = SimuleringResult(status = SimuleringStatus.OK,
        simulering = Simulering(gjelderId = "",gjelderNavn = "",datoBeregnet = LocalDate.now(),
                totalBelop = BigDecimal.TEN, periodeList = emptyList()), feilMelding = "")
val simulertOppdragEn = simuleringsResultat.copy(simulering = simuleringsResultat.simulering!!.copy(gjelderNavn = "Førstemann"))
val simulertOppdragTo = simuleringsResultat.copy(simulering = simuleringsResultat.simulering!!.copy(gjelderNavn = "Andremann"))
