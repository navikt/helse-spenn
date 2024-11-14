package no.nav.helse.spenn.avstemming

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

internal class Overføringer(rapidsConnection: RapidsConnection, private val oppdragDao: OppdragDao) : River.PacketListener {
    private companion object {
        private val logger = Logg.ny(Overføringer::class)
    }

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "oppdrag_utbetaling")
                it.requireKey("kvittering")
            }
            validate {
                it.requireKey("@id", "fødselsnummer", "utbetalingId", "fagsystemId")
                it.requireKey("avstemmingsnøkkel")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("kvittering.status", listOf("OVERFØRT", "FEIL"))
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        logger
            .offentligError("Forstod ikke kvittering på oppdrag_utbetaling (se sikkerlogg for detaljer)")
            .privatError("Forstod ikke kvittering på oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val pakkelogg = logger
            .åpent("meldingsreferanseId", packet["@id"].asText())
            .åpent("utbetalingId", packet["utbetalingId"].asText())
            .åpent("fagsystemId", packet["fagsystemId"].asText())
            .privat("fødselsnummer", packet["fødselsnummer"].asText())

        if (packet["kvittering.status"].asText() != "OVERFØRT") {
            pakkelogg.info("ignorerer kvittering pga. status er ${packet["kvittering.status"].asText()}")
            return
        }
        if (oppdragDao.oppdragOverført(packet["avstemmingsnøkkel"].asLong())) {
            pakkelogg.info("oppdrag merket som overført til OS")
        } else {
            pakkelogg.info("oppdraget ble ikke endret, er det mottatt kvittering allerede?")
        }
    }

}
