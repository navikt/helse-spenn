package no.nav.helse.spenn.overforing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.*
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SendToOSTaskTest {

    val service = OppdragStateService(
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())
    )

    val mockUtbetalingService = mock(UtbetalingService::class.java)
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    @Test
    fun afterSimuleringSendToOS() {
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetaling = behov.tilUtbetaling("12345678901")

        service.saveOppdragState(OppdragStateDTO(
            sakskompleksId = UUID.randomUUID(),
            utbetalingsreferanse = "1001",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = Status.OK),
            status = OppdragStateStatus.FERDIG
        ))
        val simulering = service.saveOppdragState(OppdragStateDTO(
            sakskompleksId = UUID.randomUUID(),
            utbetalingsreferanse = "1002",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = Status.OK),
            status = OppdragStateStatus.SIMULERING_OK
        ))
        service.saveOppdragState(OppdragStateDTO(
            sakskompleksId = UUID.randomUUID(),
            utbetalingsreferanse = "1003",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = Status.OK),
            status = OppdragStateStatus.SIMULERING_OK
        ))
        assertNotNull(simulering)
        val sendToOSTask = SendToOSTask(
            oppdragStateService = service,
            utbetalingService = mockUtbetalingService,
            meterRegistry = mockMeterRegistry
        )
        sendToOSTask.sendToOS()

        val avstemtList = service.fetchOppdragStateByAvstemtAndStatus(false, OppdragStateStatus.SENDT_OS)
        assertTrue(avstemtList.size>=2)
        val avstemming = avstemtList[0].avstemming
        assertNotNull(avstemming)
        val avstemming2 = avstemtList[1].avstemming
        assertNotNull(avstemming2)
        assertNotEquals(avstemming.oppdragStateId, avstemming2.oppdragStateId)

        val avstemt1 = avstemtList[0].let {
            it.copy(avstemming = it.avstemming!!.copy(avstemt = true))
        }
        assertNotEquals(avstemt1.id, avstemt1.avstemming!!.id, "hvis oppdragstate.id er i synk med avstemming.id KAN det være vi ikke får testet det vi vil teste")
        service.saveOppdragState(avstemt1)
        assertTrue(service.fetchOppdragStateById(avstemt1.id!!).avstemming!!.avstemt)
    }

}