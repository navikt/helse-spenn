package no.nav.helse.spenn.e2e

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.rapidApp
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class E2eTestApp {
    private val listAppender: ListAppender<ILoggingEvent> = ListAppender()
    val rapid: RepublishableTestRapid = RepublishableTestRapid()
    private val dataSource = databaseContainer.nyTilkobling()
    val database = TestDatabase(dataSource.ds)

    init {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall") as Logger
        listAppender.start()
        sikkerLogg.addAppender(listAppender)
        rapidApp(rapid, database)
        rapid.start()
    }

    fun reset() {
        databaseContainer.droppTilkobling(dataSource)
    }

    fun parseMottattLøsning(behovsvar: JsonNode): MottattLøsning {
        val løsning = behovsvar.path("@løsning").path("Utbetaling")
        return MottattLøsning(
            status = løsning.path("status").asText(),
            avstemmingsnøkkel = løsning.path("avstemmingsnøkkel").asLong()
        )
    }
    fun parseOkLøsning(behovsvar: JsonNode): OkLøsning {
        val løsning = behovsvar.path("@løsning").path("Utbetaling")
        return OkLøsning(
            status = løsning.path("status").asText(),
            overføringstidspunkt = løsning.path("overføringstidspunkt").asLocalDateTime(),
            avstemmingsnøkkel = løsning.path("avstemmingsnøkkel").asLong()
        )
    }

    fun sikkerLoggMeldinger()  =
        listAppender.list.map{it.message}


    fun parseFeilLøsning(behovsvar: JsonNode): FeilLøsning = FeilLøsning(
        status = behovsvar.path("@løsning").path("Utbetaling").path("status").asText(),
        beskrivelse = behovsvar.path("@løsning").path("Utbetaling").path("beskrivelse").asText(),
    )


    fun erLøsningOverført(index: Int): Boolean {
        val behovsvar = rapid.inspektør.message(index)
        val løsning = behovsvar.path("@løsning").path("Utbetaling")
        return løsning.path("status").asText() == "OVERFØRT"
    }


    companion object {
        fun e2eTest(f: E2eTestApp.() -> Unit) {
            E2eTestApp().apply(f).reset()
        }

        data class MottattLøsning(
            val status: String,
            val avstemmingsnøkkel: Long
        )

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

        override fun onMessage(message: String, context: MessageContext, metadata: MessageMetadata, metrics: MeterRegistry) = notifyMessage(message, context, metadata, metrics)
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
