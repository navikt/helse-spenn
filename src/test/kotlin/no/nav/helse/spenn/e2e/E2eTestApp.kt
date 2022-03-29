package no.nav.helse.spenn.e2e

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.rapidApp
import no.nav.helse.spenn.simulering.SimuleringService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class E2eTestApp(
    var rapid: RepublishableTestRapid = RepublishableTestRapid(),
    val simuleringService: SimuleringService = mockk(),
    val oppdrag: TestKø = TestKø(),
    val database: TestDatabase = TestDatabase(),
    var listAppender: ListAppender<ILoggingEvent> = ListAppender()
) {

    private fun start() {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall") as Logger
        listAppender.start()
        sikkerLogg.addAppender(listAppender)
        database.migrate()
        rapidApp(rapid, simuleringService, oppdrag, database)
        rapid.start()
    }

    private fun reset() {
        database.resetDatabase()
        rapid = RepublishableTestRapid()
        clearMocks(simuleringService)
        oppdrag.reset()
        listAppender = ListAppender()
    }


    fun parseOkLøsning(behovsvar: JsonNode): OkLøsning {
        val løsning = behovsvar["@løsning"]["Utbetaling"]
        return OkLøsning(
            status = løsning["status"].asText(),
            overføringstidspunkt = løsning["overføringstidspunkt"].asLocalDateTime(),
            avstemmingsnøkkel = løsning["avstemmingsnøkkel"].asLong()
        )
    }

    fun sikkerLoggMeldinger()  =
        listAppender.list.map{it.message}


    fun parseFeilLøsning(behovsvar: JsonNode): FeilLøsning = FeilLøsning(
        status = behovsvar["@løsning"]["Utbetaling"]["status"].asText(),
        beskrivelse = behovsvar["@løsning"]["Utbetaling"]["beskrivelse"].asText(),
    )


    fun erLøsningOverført(index: Int): Boolean {
        val behovsvar = rapid.inspektør.message(index)
        val løsning = behovsvar["@løsning"]["Utbetaling"]
        return løsning["status"].asText() == "OVERFØRT"
    }


    companion object {
        private val testEnv by lazy { E2eTestApp() }
        fun e2eTest(f: E2eTestApp.() -> Unit) {
            try {
                testEnv.start()
                f(testEnv)
            } finally {
                testEnv.reset()
            }
        }

        data class OkLøsning(
            val status: String,
            val avstemmingsnøkkel: Long,
            val overføringstidspunkt: LocalDateTime?
        )

        data class FeilLøsning(
            val status: String,
            val beskrivelse: String
        )
    }

    internal class RepublishableTestRapid(private val rapid: TestRapid = TestRapid()) : RapidsConnection(), RapidsConnection.StatusListener, RapidsConnection.MessageListener {
        val inspektør get() = rapid.inspektør

        init {
            rapid.register(this as StatusListener)
            rapid.register(this as MessageListener)
        }

        override fun onMessage(message: String, context: MessageContext) = notifyMessage(message, context)
        override fun onNotReady(rapidsConnection: RapidsConnection) = notifyNotReady()
        override fun onReady(rapidsConnection: RapidsConnection) = notifyReady()
        override fun onShutdown(rapidsConnection: RapidsConnection) = notifyShutdown()
        override fun onStartup(rapidsConnection: RapidsConnection) = notifyStartup()

        fun sendTestMessage(message: String) = rapid.sendTestMessage(message)

        override fun publish(message: String) {
            rapid.publish(message)
            rapid.sendTestMessage(message)
        }

        override fun publish(key: String, message: String) {
            rapid.publish(key, message)
            rapid.sendTestMessage(message)
        }

        override fun rapidName() = rapid.rapidName()
        override fun start() = rapid.start()
        override fun stop() = rapid.start()
    }
}
