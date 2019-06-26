package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.tasks.SendToOSTask
import no.nav.helse.spenn.vedtak.UtbetalingService
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.context.annotation.ComponentScan
import java.util.*
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

    val utbetalingServiceMock = Mockito.mock(UtbetalingService::class.java)

    @Test
    fun afterSimuleringSendToOS() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling()

        val simulering = service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadKey, utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.SIMULERING_OK))
        val another = service.saveOppdragState(OppdragStateDTO(
                soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.SIMULERING_OK
        ))
        assertNotNull(simulering)
        val sendToOSTask = SendToOSTask(oppdragStateService = service, utbetalingService = utbetalingServiceMock)
        sendToOSTask.sendToOS()
        val avstemtList = service.fetchOppdragStateByAvstemtAndStatus(false, OppdragStateStatus.SENDT_OS)
        assertTrue(avstemtList.size>=2)
        val avstemming = avstemtList.get(0).avstemming
        assertNotNull(avstemming)
        println("avstemming: ${avstemming.id} oppdragstateId: ${avstemming.oppdragStateId} nøkkel: ${avstemming.nokkel} avstemt: ${avstemming.avstemt}")
        val avstemming2 = avstemtList.get(1).avstemming
        assertNotNull(avstemming2)
        println("avstemming: ${avstemming2.id} oppdragstateId: ${avstemming2.oppdragStateId} nøkkel: ${avstemming2.nokkel} avstemt: ${avstemming2.avstemt}")

    }

}