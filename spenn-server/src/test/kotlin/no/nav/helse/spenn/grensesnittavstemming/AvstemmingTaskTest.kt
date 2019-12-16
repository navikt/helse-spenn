package no.nav.helse.spenn.grensesnittavstemming

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.AvstemmingMapper
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.SpennOppdragFactory
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.jms.Connection
import javax.jms.Session
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AvstemmingTaskTest {

    private val service = OppdragService(TestDb.createMigratedDataSource())
    private val mockMeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())

    val mockConnection: Connection = Mockito.mock(Connection::class.java)
    private val mockJmsSession: Session = Mockito.mock(Session::class.java)

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
        val behov = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val utbetalingTemplate = SpennOppdragFactory.lagOppdragFraBehov(behov, "12345678901")

        service.lagreNyttOppdrag(utbetalingTemplate.copy(utbetalingsreferanse = "2001"))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(TransaksjonStatus.FERDIG, lagOppdragResponseXml("whatever", false, "00")!!, feilmelding = null)
        }

        service.lagreNyttOppdrag(utbetalingTemplate.copy(utbetalingsreferanse = "2002"))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(TransaksjonStatus.FERDIG, lagOppdragResponseXml("whatever", false, "00")!!, feilmelding = null)
        }

        service.lagreNyttOppdrag(utbetalingTemplate.copy(utbetalingsreferanse = "2003"))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(TransaksjonStatus.FERDIG, lagOppdragResponseXml("whatever", false, "04")!!, feilmelding = null)
        }

        val sendteMeldinger = mutableListOf<Avstemmingsdata>()

        class MockSender : AvstemmingMQSender(mockConnection, "tullequeue",
            JAXBAvstemmingsdata()
        ){
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                sendteMeldinger.add(avstemmingsMelding)
            }
        }

        val loglog = createLogAppender()

        SendTilAvstemmingTask(service, MockSender(), mockMeterRegistry, marginInHours = 0)
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
            assertEquals(utbetalingTemplate.utbetaling!!.utbetalingsLinjer.first().sats.toLong() * 3, this.total.totalBelop.toLong())

            assertEquals(1, this.detalj.size)
            assertEquals("04", this.detalj.first().alvorlighetsgrad)
            assertEquals("2003", this.detalj.first().avleverendeTransaksjonNokkel)

        }
        assertEquals(AksjonType.AVSL, sendteMeldinger.last().aksjon.aksjonType)

        val avleverendeAvstemmingId = sendteMeldinger.first().aksjon.avleverendeAvstemmingId!!
        val loggMeldinger = loglog.list.filter {
            it.level == Level.INFO && it.message.contains("avleverendeAvstemmingId=$avleverendeAvstemmingId")
        }
        assertEquals(1, loggMeldinger.size, "Det skal logges en linje med avleverendeAvstemmingId på INFO-nivå")
        assertTrue(loggMeldinger.first().message.contains("nokkelFom="))
        assertTrue(loggMeldinger.first().message.contains("nokkelTom="))

        assertTrue(service.hentEnnåIkkeAvstemteTransaksjonerEldreEnn(LocalDateTime.now()).isEmpty(),
                "Det skal ikke være igjen noen ikke-avstemte meldinger")
    }

    private fun lagOppdragResponseXml(fagsystemId:String, manglerRespons:Boolean=false, alvorlighetsgrad: String) : String? {
        if (manglerRespons) {
            return null
        }
        val kvittering = Oppdrag()
        kvittering.mmel = Mmel()
        kvittering.mmel.kodeMelding = "Melding"
        kvittering.mmel.alvorlighetsgrad = alvorlighetsgrad
        kvittering.mmel.beskrMelding = "Beskrivelse"
        kvittering.oppdrag110 = Oppdrag110()
        kvittering.oppdrag110.fagsystemId = fagsystemId
        return JAXBOppdrag().fromOppdragToXml(kvittering)
    }


    private fun settAltEksisterendeTilAvstemt() {
        service.hentEnnåIkkeAvstemteTransaksjonerEldreEnn(LocalDateTime.now()).forEach {
            it.markerSomAvstemt()
        }
    }

    private fun createLogAppender(): ListAppender<ILoggingEvent> =
        ListAppender<ILoggingEvent>().apply {
            (LoggerFactory.getLogger(AvstemmingMapper::class.java.name) as Logger).addAppender(this)
            start()
        }

}
