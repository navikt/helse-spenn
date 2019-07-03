package no.nav.helse.spenn.grensesnittavstemming

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.tasks.SendTilAvstemmingTask
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.jms.core.JmsTemplate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataJdbcTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])
@ImportAutoConfiguration(classes = [JooqAutoConfiguration::class])
@ComponentScan(basePackages = ["no.nav.helse.spenn.dao"])
class AvstemmingTaskTest {

    @Autowired
    lateinit var service: OppdragStateService

    @BeforeEach
    fun beforeEach() {
        settAltEksisterendeTilAvstemt()
    }

    @Test
    fun ingenOppdragSkalBliIngenAvstemming() {
        class MockSender : AvstemmingMQSender(Mockito.mock(JmsTemplate::class.java), "tullekø", JAXBAvstemmingsdata() ){
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                assertFalse(true, "Skal ikke bli sendt noen avstemmingsmeldinger")
            }
        }
        val sendTilAvstemmingTask = SendTilAvstemmingTask(service, MockSender())
        sendTilAvstemmingTask.sendTilAvstemming()
    }

    @Test
    fun testAtDetSendesLoggesOgOppdateresAvstemminger() {
        val soknadKey = UUID.randomUUID()
        val soknadKey2 = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")

        service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadKey, utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.FERDIG,
                avstemming = AvstemmingDTO(
                        id = 123L,
                        avstemt = false,
                        nokkel = LocalDateTime.now().minusHours(2)
                )))
        service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadKey2, utbetalingsOppdrag = utbetaling,
                simuleringResult = SimuleringResult(status = Status.OK),
                status = OppdragStateStatus.FERDIG,
                avstemming = AvstemmingDTO(
                        id = 124L,
                        avstemt = false,
                        nokkel = LocalDateTime.now().minusHours(2)
                )))

        val sendteMeldinger = mutableListOf<Avstemmingsdata>()

        class MockSender : AvstemmingMQSender(Mockito.mock(JmsTemplate::class.java), "tullekø", JAXBAvstemmingsdata() ){
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                sendteMeldinger.add(avstemmingsMelding)
            }
        }

        val loglog = createLogAppender()

        SendTilAvstemmingTask(service, MockSender())
                .sendTilAvstemming()

        assertEquals(3, sendteMeldinger.size)
        assertEquals(AksjonType.START, sendteMeldinger.first().aksjon.aksjonType)
        sendteMeldinger[1].apply {
            assertEquals(AksjonType.DATA, this.aksjon.aksjonType)
            assertEquals(2, this.total.totalAntall)
            assertEquals(utbetaling.utbetalingsLinje.first().sats.toLong() * 2, this.total.totalBelop.toLong())
        }
        assertEquals(AksjonType.AVSL, sendteMeldinger.last().aksjon.aksjonType)

        assertTrue(loglog.list.filter {
                it.level == Level.INFO && it.message.startsWith("Sender avstemmingsmeldinger med avleverendeAvstemmingId=")
        }.isNotEmpty(), "Det skal logges avleverendeAvstemmingId på INFO-nivå")

        assertTrue(service.fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(LocalDateTime.now()).isEmpty(),
                "Det skal ikke være igjen noen ikke-avstemte meldinger")
    }

    //////////////////////////////////
    //////////////////////////////////
    //////////////////////////////////

    private fun settAltEksisterendeTilAvstemt() =
        service.fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(LocalDateTime.now()).forEach {
            service.saveOppdragState(it.copy(avstemming = it.avstemming!!.copy(avstemt = true)))
        }

    private fun createLogAppender(): ListAppender<ILoggingEvent> =
        ListAppender<ILoggingEvent>().apply {
            (LoggerFactory.getLogger(SendTilAvstemmingTask::class.java.name) as Logger).addAppender(this)
            start()
        }

}