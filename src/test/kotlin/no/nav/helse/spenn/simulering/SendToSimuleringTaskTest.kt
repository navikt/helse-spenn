package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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
                meterRegistry = mockMeterRegistry, oppdragStateService = mockPersistence)
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        node.tilVedtak(soknadKey.toString())

        `when`(mockPersistence.fetchOppdragStateByStatus(OppdragStateStatus.STARTET, 100)).thenReturn(listOf(oppdragEn, oppdragTo))
        `when`(mockSimuleringService.runSimulering(oppdragEn)).thenReturn(simulertOppdragEn)
        `when`(mockSimuleringService.runSimulering(oppdragTo)).thenReturn(simulertOppdragTo)
        `when`(mockPersistence.saveOppdragState(simulertOppdragEn)).thenReturn(simulertOppdragEn)
        `when`(mockPersistence.saveOppdragState(simulertOppdragTo)).thenReturn(simulertOppdragTo)

        sendToSimuleringTask.sendSimulering()

        verify(mockPersistence, times(2)).saveOppdragState(any())
    }

    fun <T> any(): T = Mockito.any<T>()
}

val oppdragEn = OppdragStateDTO(
        id = 1L,
        status = OppdragStateStatus.STARTET,
        simuleringResult = null,
        oppdragResponse = null,
        modified = LocalDateTime.now(),
        created = LocalDateTime.now(),
        avstemming = null,
        soknadId = UUID.randomUUID(),
        utbetalingsOppdrag = UtbetalingsOppdrag(
                vedtak = Vedtak(
                        soknadId = UUID.randomUUID(),
                        maksDato = LocalDate.now().plusYears(1),
                        aktorId = "12341234",
                        vedtaksperioder = emptyList()
                ),
                utbetalingsLinje = emptyList(),
                oppdragGjelder = "someone",
                operasjon = AksjonsKode.SIMULERING
        )
)
val simuleringsResultat = SimuleringResult(status = Status.OK, mottaker = Mottaker(datoBeregnet = "", gjelderId = "", gjelderNavn = "",
        periodeList = emptyList(), totalBelop = BigDecimal.TEN, kodeFaggruppe = "KODE"), feilMelding = "")
val simulertOppdragEn = oppdragEn.copy(simuleringResult = simuleringsResultat)
val oppdragTo = oppdragEn.copy(id = 2L, soknadId = UUID.randomUUID())
val simulertOppdragTo = oppdragTo.copy(simuleringResult = simuleringsResultat)