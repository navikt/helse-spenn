package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.simulering.Mottaker
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.tasks.SendToSimuleringTask
import no.nav.helse.spenn.vedtak.UtbetalingService
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals

@JooqTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])
@ComponentScan(basePackages = ["no.nav.helse.spenn.dao"])
class SendToSimuleringTaskTest {

    @Autowired
    lateinit var service: OppdragStateService

    val mockSimuleringService = mock(SimuleringService::class.java)
    val utbetalingService = UtbetalingService(simuleringService = mockSimuleringService,
            oppdragSender = mock(OppdragMQSender::class.java))
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())



    @Test
    fun sendToSimulering() {
        val sendToSimuleringTask = SendToSimuleringTask(utbetalingService = utbetalingService,
                meterRegistry = mockMeterRegistry, oppdragStateService = service)
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")

        service.saveOppdragState(OppdragStateDTO(
                soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling))
        service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadKey, utbetalingsOppdrag = utbetaling))
        service.saveOppdragState(OppdragStateDTO(
                soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.SIMULERING_OK
        ))
        `when`(mockSimuleringService.simulerOppdrag(any())).thenReturn(SimuleringResult(status=Status.OK, mottaker = Mottaker(datoBeregnet = "",totalBelop = BigDecimal.TEN, gjelderId = "", gjelderNavn = "", periodeList = emptyList())))
        sendToSimuleringTask.sendSimulering()
        assertEquals(service.fetchOppdragState(soknadKey).status, OppdragStateStatus.SIMULERING_OK, "Simulering should now be ok")
    }

    fun <T> any(): T = Mockito.any<T>()


}