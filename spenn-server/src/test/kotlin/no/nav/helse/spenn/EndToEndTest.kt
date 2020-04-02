package no.nav.helse.spenn

import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import no.nav.helse.rapids_rivers.inMemoryRapid
import no.nav.helse.spenn.core.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.oppdrag.OppdragXml
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import javax.jms.*
import javax.jms.Queue
import kotlin.test.assertEquals
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse as SimulerBeregningResponseWrapper

@KtorExperimentalAPI
class EndToEndTest {

    private val simulerFpService = mockk<SimulerFpService>()
    private val simuleringService = SimuleringService(simulerFpService)
    private val mqConnection = mockk<Connection>()
    private val mqSession = mockk<Session>()
    private val dataSource = TestDb.createMigratedDataSource()
    private val oppdragService = OppdragService(dataSource)
    private val rapidsConnection = inMemoryRapid { }
    private val mqQueuesEnv = MqQueuesEnvironment(
        oppdragQueueSend = "oppdragSend",
        oppdragQueueMottak = "oppdragMottak",
        avstemmingQueueSend = "avstemmingSend"
    )

    private fun consumer(queueName: String): CapturingSlot<MessageListener> {
        val slot = slot<MessageListener>()
        val mockQueue = mockk<Queue>()
        val mockConsumer = mockk<MessageConsumer>()

        every { mqSession.createQueue(queueName) } returns mockQueue
        every { mqSession.createConsumer(mockQueue) } returns mockConsumer
        every { mockConsumer.messageListener = capture(slot) } returns Unit

        return slot
    }

    private fun producer(queueName: String): MessageProducer {
        val mockQueue = mockk<Queue>()
        val mockProducer = mockk<MessageProducer>()

        every { mqSession.createQueue(queueName) } returns mockQueue
        every { mqSession.createProducer(mockQueue) } returns mockProducer

        return mockProducer
    }

    private val oppdragProducer = producer(mqQueuesEnv.oppdragQueueSend)
    private val oppdragConsumer = consumer(mqQueuesEnv.oppdragQueueMottak)
    private val avstemmingProducer = producer(mqQueuesEnv.avstemmingQueueSend)

    @Test
    internal fun test() {

        every { mqConnection.createSession() } returns mqSession
        every { mqConnection.close() } returns Unit
        every { mqConnection.start() } returns Unit
        every { simulerFpService.simulerBeregning(any()) } returns SimulerBeregningResponseWrapper().apply {
            this.response = SimulerBeregningResponse().apply {
                this.simulering = Beregning().apply {
                    this.belop = "1000.0".toBigDecimal()
                    this.beregningsPeriode.addAll(listOf())
                    this.datoBeregnet = LocalDate.now().toString()
                    this.gjelderId = "idk"
                    this.gjelderNavn = "navn"
                    this.kodeFaggruppe = "idk"
                }
            }
        }

        every { oppdragProducer.send(any()) } returns Unit

        launchApplication(
            simuleringService = simuleringService,
            mqConnection = mqConnection,
            dataSource = dataSource,
            oppdragService = oppdragService,
            rapidsConnection = rapidsConnection,
            mqQueues = mqQueuesEnv
        )

        val osMessageSlot = slot<String>()
        every { mqSession.createTextMessage(capture(osMessageSlot)) } returns mockk<TextMessage>().apply {
            every { setJMSReplyTo(any()) } returns Unit
        }

        rapidsConnection.sendToListeners(utbetalingsbehov())

        verify(timeout = 30000, exactly = 1) { oppdragProducer.send(any()) }
        verify(timeout = 30000, exactly = 1) { simulerFpService.simulerBeregning(any()) }

        val oppdrag = OppdragXml.unmarshal(osMessageSlot.captured)
        oppdrag.mmel = Mmel().apply { alvorlighetsgrad = KvitteringAlvorlighetsgrad.OK.kode }

        val msg = OppdragXml.marshal(oppdrag).replace("ns2:oppdrag xmlns:ns2", "oppdrag xmlns")
        oppdragConsumer.captured.onMessage(mockk<TextMessage>().apply {
            every { getBody(String::class.java) } returns msg
        })

        assertEquals(1, rapidsConnection.outgoingMessages.size)
    }

    private fun utbetalingsbehov(): String {
        return defaultObjectMapper.writeValueAsString(
            mapOf(
                "@behov" to listOf("Utbetaling"),
                "@id" to UUID.randomUUID().toString(),
                "fødselsnummer" to "fnr",
                "utbetalingsreferanse" to "ref",
                "utbetalingslinjer" to listOf(
                    mapOf(
                        "id" to "idk",
                        "dagsats" to "1000.0",
                        "satsTypeKode" to "DAG",
                        "fom" to "2020-04-20",
                        "tom" to "2020-05-20",
                        "organisasjonsnummer" to "420699001",
                        "grad" to 100,
                        "erEndring" to false
                    )
                ),
                "maksdato" to "2020-04-20",
                "organisasjonsnummer" to "420699001",
                "saksbehandler" to "Z007"
            )
        )
    }
}
