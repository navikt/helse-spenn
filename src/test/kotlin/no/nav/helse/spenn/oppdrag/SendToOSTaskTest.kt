package no.nav.helse.spenn.oppdrag


import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.dao.toEntity
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.tasks.SendToOSTask
import no.nav.helse.spenn.vedtak.UtbetalingService
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.context.annotation.ComponentScan
import java.util.*
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DataJdbcTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])
@ImportAutoConfiguration(classes = [JooqAutoConfiguration::class])
@ComponentScan(basePackages = ["no.nav.helse.spenn.dao"])
class SimuleringToAvstemmingScenarioTest {

    @Autowired
    lateinit var service: OppdragStateService
    @Autowired
    lateinit var repository: OppdragStateJooqRepository

    val mockUtbetalingService = mock(UtbetalingService::class.java)
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    @Test
    fun afterSimuleringSendToOS() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")

        service.saveOppdragState(OppdragStateDTO(
                soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.FERDIG
        ))
        val simulering = service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadKey, utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.SIMULERING_OK))
        service.saveOppdragState(OppdragStateDTO(
                soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.SIMULERING_OK
        ))
        assertNotNull(simulering)
        val sendToOSTask = SendToOSTask(oppdragStateService = service, utbetalingService = mockUtbetalingService,
                meterRegistry = mockMeterRegistry)
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
        repository.update(toEntity(avstemt1))
        assertTrue(service.fetchOppdragStateById(avstemt1.id!!).avstemming!!.avstemt)
    }

}