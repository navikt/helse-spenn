package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory
import java.util.*

internal class Overføringer(rapidsConnection: RapidsConnection, private val oppdragDao: OppdragDao) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(Overføringer::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "oppdrag_utbetaling")
                it.requireKey("kvittering")
            }
            validate {
                it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "utbetalingId", "fagsystemId", "avstemmingsnøkkel")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("kvittering.status", "kvittering.beskrivelse")
                it.requireAny("kvittering.status", listOf("OVERFØRT", "FEIL"))
                it.interestedIn("kvittering.feilmelding", "kvittering.xmlmelding")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Forstod ikke kvittering på oppdrag_utbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke kvittering på oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val fagsystemId = packet["fagsystemId"].asText().trim()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val beskrivelse = packet["kvittering.beskrivelse"].asText()
        val status = Oppdragstatus.valueOf(packet["kvittering.status"].asText())

        val oppdrag = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId, fagsystemId) ?: return sikkerLogg.error("oppdrag finnes ikke for utbetalingId=$utbetalingId fagsystemId=$fagsystemId")
        if (oppdrag.erKvittert()) {
            log.info("oppdaterer ikke status for utbetaling $utbetalingId fagsystemId=$fagsystemId status=$status $beskrivelse")
            sikkerLogg.info("oppdaterer ikke status for utbetaling $utbetalingId fagsystemId=$fagsystemId status=$status $beskrivelse",
                keyValue("fødselsnummer", fødselsnummer)
            )
            return
        }

        log.info("oppdaterer status for utbetaling $utbetalingId fagsystemId=$fagsystemId status=$status $beskrivelse")
        sikkerLogg.info("oppdaterer status for utbetaling $utbetalingId fagsystemId=$fagsystemId status=$status $beskrivelse",
            keyValue("fødselsnummer", fødselsnummer)
        )
        oppdragDao.oppdaterOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, status)

        oppdragDao.hentBehovForOppdrag(utbetalingId, fagsystemId)
            ?.apply {
                this["@løsning"] = mapOf(
                    behovnavn to mapOf(
                        "status" to status,
                        "beskrivelse" to status.beskrivelse(),
                        "overføringstidspunkt" to packet["@opprettet"].asText(),
                        "avstemmingsnøkkel" to avstemmingsnøkkel
                    )
                )
            }
            ?.also { behovMedLøsning ->
                context.publish(behovMedLøsning.toJson())
            }
    }
}
