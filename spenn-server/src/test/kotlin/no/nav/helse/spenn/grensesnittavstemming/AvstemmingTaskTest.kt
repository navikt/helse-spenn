package no.nav.helse.spenn.grensesnittavstemming

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.avstemming.AvstemmingMapperTest
import no.nav.helse.spenn.avstemmingsnokkelFormatter
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.TransaksjonDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.jms.Connection
import javax.jms.Session
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvstemmingTaskTest {

    val service = OppdragStateService(
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())
    )
    val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    val mockConnection = Mockito.mock(Connection::class.java)
    val mockJmsSession = Mockito.mock(Session::class.java)

    @BeforeEach
    fun beforeEach() {
        settAltEksisterendeTilAvstemt()
        Mockito.`when`(mockConnection.createSession()).thenReturn(mockJmsSession)
    }

    @Test
    fun ingenOppdragSkalBliIngenAvstemming() {
        class MockSender : AvstemmingMQSender(mockConnection, "tullequeue",
            JAXBAvstemmingsdata()
        ){
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                assertFalse(true, "Skal ikke bli sendt noen avstemmingsmeldinger")
            }
        }
        val sendTilAvstemmingTask = SendTilAvstemmingTask(service, MockSender(), mockMeterRegistry)
        sendTilAvstemmingTask.sendTilAvstemming()
    }

    @Test
    fun testAtDetSendesLoggesOgOppdateresAvstemminger() {
        val soknadKey = UUID.randomUUID()
        val soknadKey2 = UUID.randomUUID()
        val soknadKey3 = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetaling = behov.tilUtbetaling("12345678901")

        val oppdrag1 = service.saveOppdragState(TransaksjonDTO(
            sakskompleksId = soknadKey,
            utbetalingsreferanse = "2001",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = SimuleringStatus.OK),
            status = OppdragStateStatus.FERDIG,
            oppdragResponse = AvstemmingMapperTest.lagOppdragResponseXml("whatever", false, "00"),
            avstemming = AvstemmingDTO(
                id = 123L,
                avstemt = false,
                nokkel = LocalDateTime.now().minusHours(2)
            )
        ))
        service.saveOppdragState(TransaksjonDTO(
            sakskompleksId = soknadKey2,
            utbetalingsreferanse = "2002",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = SimuleringStatus.OK),
            status = OppdragStateStatus.FERDIG,
            oppdragResponse = AvstemmingMapperTest.lagOppdragResponseXml("whatever", false, "00"),
            avstemming = AvstemmingDTO(
                id = 124L,
                avstemt = false,
                nokkel = LocalDateTime.now().minusHours(2).plusMinutes(1)
            )
        ))
        val oppdrag3 = service.saveOppdragState(TransaksjonDTO(
            sakskompleksId = soknadKey3,
            utbetalingsreferanse = "2003",
            utbetalingsOppdrag = utbetaling,
            simuleringResult = SimuleringResult(status = SimuleringStatus.OK),
            status = OppdragStateStatus.FERDIG,
            oppdragResponse = AvstemmingMapperTest.lagOppdragResponseXml("whatever", false, "04"),
            avstemming = AvstemmingDTO(
                id = 125L,
                avstemt = false,
                nokkel = LocalDateTime.now().minusHours(2).plusMinutes(2)
            )
        ))

        val sendteMeldinger = mutableListOf<Avstemmingsdata>()

        class MockSender : AvstemmingMQSender(mockConnection, "tullequeue",
            JAXBAvstemmingsdata()
        ){
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                sendteMeldinger.add(avstemmingsMelding)
            }
        }

        val loglog = createLogAppender()

        SendTilAvstemmingTask(service, MockSender(), mockMeterRegistry)
                .sendTilAvstemming()

        assertEquals(3, sendteMeldinger.size)
        assertEquals(AksjonType.START, sendteMeldinger.first().aksjon.aksjonType)
        sendteMeldinger[1].apply {
            assertEquals(AksjonType.DATA, this.aksjon.aksjonType)
            assertEquals(3, this.total.totalAntall)
            assertEquals(2, this.grunnlag.godkjentAntall)
            assertEquals(1, this.grunnlag.varselAntall)
            assertEquals(0, this.grunnlag.avvistAntall)
            assertEquals(0, this.grunnlag.manglerAntall)
            assertEquals(utbetaling.utbetalingsLinje.first().sats.toLong() * 3, this.total.totalBelop.toLong())

            assertEquals(1, this.detalj.size)
            assertEquals("04", this.detalj.first().alvorlighetsgrad)
            assertEquals(oppdrag3.utbetalingsreferanse, this.detalj.first().avleverendeTransaksjonNokkel)

        }
        assertEquals(AksjonType.AVSL, sendteMeldinger.last().aksjon.aksjonType)

        val avleverendeAvstemmingId = sendteMeldinger.first().aksjon.avleverendeAvstemmingId!!
        val loggMeldinger = loglog.list.filter {
            it.level == Level.INFO && it.message.contains("avleverendeAvstemmingId=$avleverendeAvstemmingId")
        }
        assertEquals(1, loggMeldinger.size, "Det skal logges en linje med avleverendeAvstemmingId på INFO-nivå")
        assertTrue(loggMeldinger.first().message.contains("nokkelFom=${oppdrag1.avstemming!!.nokkel.format(avstemmingsnokkelFormatter)}"))
        assertTrue(loggMeldinger.first().message.contains("nokkelTom=${oppdrag3.avstemming!!.nokkel.format(avstemmingsnokkelFormatter)}"))

        assertTrue(service.fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(LocalDateTime.now()).isEmpty(),
                "Det skal ikke være igjen noen ikke-avstemte meldinger")
    }

    private fun settAltEksisterendeTilAvstemt() {
        service.fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(LocalDateTime.now()).forEach {
            service.saveOppdragState(it.copy(avstemming = it.avstemming!!.copy(avstemt = true)))
        }
    }

    private fun createLogAppender(): ListAppender<ILoggingEvent> =
        ListAppender<ILoggingEvent>().apply {
            (LoggerFactory.getLogger(SendTilAvstemmingTask::class.java.name) as Logger).addAppender(this)
            start()
        }

}