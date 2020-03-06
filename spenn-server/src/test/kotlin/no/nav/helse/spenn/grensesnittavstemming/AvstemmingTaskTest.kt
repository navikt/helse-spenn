package no.nav.helse.spenn.grensesnittavstemming

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.oppdrag.AvstemmingMapper
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.utbetalingMedRef
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.jms.Connection
import javax.jms.Session
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AvstemmingTaskTest {

    private val service = OppdragService(TestDb.createMigratedDataSource())

    private val mockConnection = mockk<Connection>().apply {
        every { createSession() } returns mockk<Session>().apply {
            every { createQueue(any()) } returns mockk()
            every { createProducer(any()) } returns mockk()
        }
    }

    @BeforeEach
    fun beforeEach() {
        settAltEksisterendeTilAvstemt()

    }

    @Test
    fun ingenOppdragSkalBliIngenAvstemming() {
        class MockSender : AvstemmingMQSender(
            mockConnection, "tullequeue",
            JAXBAvstemmingsdata()
        ) {
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                assertFalse(true, "Skal ikke bli sendt noen avstemmingsmeldinger")
            }
        }

        val sendTilAvstemmingTask = SendTilAvstemmingTask(service, MockSender())
        sendTilAvstemmingTask.sendTilAvstemming()
    }

    @Test
    fun testAtDetSendesLoggesOgOppdateresAvstemminger() {
        val dagsatser = listOf(
            1000.0,
            900.0,
            800.0
        )

        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "2001", dagsats = dagsatser[0]))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(
                TransaksjonStatus.FERDIG,
                lagOppdragResponseXml("whatever", false, "00")!!,
                feilmelding = null
            )
        }

        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "2002", dagsats = dagsatser[1]))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(
                TransaksjonStatus.FERDIG,
                lagOppdragResponseXml("whatever", false, "00")!!,
                feilmelding = null
            )
        }

        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "2003", dagsats = dagsatser[2]))
        service.hentNyeOppdrag(5).first().apply {
            forberedSendingTilOS()
            lagreOSResponse(
                TransaksjonStatus.FERDIG,
                lagOppdragResponseXml("whatever", false, "04")!!,
                feilmelding = null
            )
        }

        val sendteMeldinger = mutableListOf<Avstemmingsdata>()

        class MockSender : AvstemmingMQSender(mockConnection, "tullequeue", JAXBAvstemmingsdata()) {
            override fun sendAvstemmingsmelding(avstemmingsMelding: Avstemmingsdata) {
                sendteMeldinger.add(avstemmingsMelding)
            }
        }

        val loglog = createLogAppender()

        SendTilAvstemmingTask(service, MockSender(), marginInHours = 0)
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
            assertEquals(dagsatser.sum().toLong(), this.total.totalBelop.toLong())

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

        assertTrue(
            service.hentEnnåIkkeAvstemteTransaksjonerEldreEnn(LocalDateTime.now()).isEmpty(),
            "Det skal ikke være igjen noen ikke-avstemte meldinger"
        )
    }

    private fun lagOppdragResponseXml(
        fagsystemId: String,
        manglerRespons: Boolean = false,
        alvorlighetsgrad: String
    ): String? {
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
        return JAXBOppdrag.fromOppdragToXml(kvittering)
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
