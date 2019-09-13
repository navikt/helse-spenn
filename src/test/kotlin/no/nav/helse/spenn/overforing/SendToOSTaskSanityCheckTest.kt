package no.nav.helse.spenn.overforing

import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.any
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.vedtak.Vedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

class SendToOSTaskSanityCheckTest {

    val maksDagsats = BigDecimal(100000 * 6.5 / 260)

    @Test
    fun dagssatsSomIkkeErOverMaksSkalIkkeBliStoppet() {
        val mockUtbetalingService = mock(UtbetalingService::class.java)
        val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())
        val mockOppdragStateService = mock(OppdragStateService::class.java)

        val ostask = SendToOSTask(mockOppdragStateService, mockUtbetalingService, mockMeterRegistry, 100)

        val oppdrag = oppdragMedSats(maksDagsats.toLong())

        `when`(mockOppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK,100))
                .thenReturn(listOf(oppdrag))

        ostask.sendToOS()

        verify(mockUtbetalingService).sendUtbetalingOppdragMQ(any())
    }

    @Test
    fun dagsatsOverMaksSkalBliStoppet() {
        val mockUtbetalingService = mock(UtbetalingService::class.java)
        val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())
        val mockOppdragStateService = mock(OppdragStateService::class.java)

        val ostask = SendToOSTask(mockOppdragStateService, mockUtbetalingService, mockMeterRegistry, 100)

        val oppdrag = oppdragMedSats(maksDagsats.toLong() + 1)

        `when`(mockOppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK,100))
                .thenReturn(listOf(oppdrag))

        ostask.sendToOS()

        verify(mockUtbetalingService, never()).sendUtbetalingOppdragMQ(any())
    }

    @Test
    fun annenSatstypeEnnDagsatsSkalBliStoppet() {
        val mockUtbetalingService = mock(UtbetalingService::class.java)
        val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())
        val mockOppdragStateService = mock(OppdragStateService::class.java)

        val ostask = SendToOSTask(mockOppdragStateService, mockUtbetalingService, mockMeterRegistry, 100)

        SatsTypeKode.values().filter { it != SatsTypeKode.DAGLIG }.forEach { satsTypeKode ->
            val oppdrag = oppdragMedSats(1000, satsTypeKode)
            `when`(mockOppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK,100))
                    .thenReturn(listOf(oppdrag))
            ostask.sendToOS()
            verify(mockUtbetalingService, never()).sendUtbetalingOppdragMQ(any())
        }

    }


    private fun oppdragMedSats(sats: Long, satsTypeKode : SatsTypeKode = SatsTypeKode.DAGLIG) =
            OppdragStateDTO(
                    soknadId = UUID.randomUUID(), utbetalingsOppdrag = UtbetalingsOppdrag(
                    vedtak = Vedtak(
                            soknadId = UUID.randomUUID(),
                            maksDato = LocalDate.now().plusYears(1),
                            vedtaksperioder = emptyList(),
                            aktorId = "111122223333",
                            saksbehandler = "SPA"
                    ),
                    oppdragGjelder = "12345678901",
                    operasjon = AksjonsKode.OPPDATER,
                    utbetalingsLinje = listOf(
                            UtbetalingsLinje(id = "1",
                                    satsTypeKode = satsTypeKode,
                                    sats = BigDecimal.valueOf(sats),
                                    utbetalesTil = "999888777",
                                    datoFom = LocalDate.now().minusWeeks(4),
                                    datoTom = LocalDate.now().minusWeeks(1),
                                    grad = BigInteger.valueOf(100)
                            )
                    )
            ))

}