package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.any
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.TransaksjonDTO
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SendToSimuleringTaskTest {

    val mockPersistence = mock(OppdragStateService::class.java)
    val mockSimuleringService = mock(SimuleringService::class.java)
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    @Test
    fun sendToSimulering() {
        val sendToSimuleringTask = SendToSimuleringTask(simuleringService = mockSimuleringService,
                meterRegistry = mockMeterRegistry, oppdragService = mockPersistence)
        `when`(mockPersistence.fetchOppdragStateByStatus(OppdragStateStatus.STARTET, 100)).thenReturn(listOf(oppdragEn, oppdragTo))
        `when`(mockSimuleringService.runSimulering(oppdragEn)).thenReturn(simulertOppdragEn)
        `when`(mockSimuleringService.runSimulering(oppdragTo)).thenReturn(simulertOppdragTo)
        `when`(mockPersistence.saveOppdragState(simulertOppdragEn)).thenReturn(simulertOppdragEn)
        `when`(mockPersistence.saveOppdragState(simulertOppdragTo)).thenReturn(simulertOppdragTo)

        sendToSimuleringTask.sendSimulering()

        verify(mockPersistence, times(2)).saveOppdragState(any())
    }


}

val oppdragEn = TransaksjonDTO(
        id = 1L,
        status = OppdragStateStatus.STARTET,
        simuleringResult = null,
        oppdragResponse = null,
        modified = LocalDateTime.now(),
        created = LocalDateTime.now(),
        avstemming = null,
        sakskompleksId = UUID.randomUUID(),
        utbetalingsreferanse = "1001",
        utbetalingsOppdrag = UtbetalingsOppdrag(
                behov = etEnkeltBehov(),
                utbetalingsLinje = emptyList(),
                oppdragGjelder = "someone",
                operasjon = AksjonsKode.SIMULERING
        )
)
val simuleringsResultat = SimuleringResult(status = SimuleringStatus.OK,
        simulering = Simulering(gjelderId = "",gjelderNavn = "",datoBeregnet = LocalDate.now(),
                totalBelop = BigDecimal.TEN, periodeList = emptyList()), feilMelding = "")
val simulertOppdragEn = oppdragEn.copy(simuleringResult = simuleringsResultat)
val oppdragTo = oppdragEn.copy(id = 2L, sakskompleksId = UUID.randomUUID())
val simulertOppdragTo = oppdragTo.copy(simuleringResult = simuleringsResultat)