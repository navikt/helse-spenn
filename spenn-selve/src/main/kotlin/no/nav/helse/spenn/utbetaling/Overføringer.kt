package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.util.*

internal class Overføringer(rapidsConnection: RapidsConnection, private val oppdragDao: OppdragDao) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(Overføringer::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppdrag_utbetaling")
                it.demandKey("kvittering")
                it.requireKey("@id", "aktørId", "fødselsnummer", "organisasjonsnummer", "utbetalingId", "fagsystemId", "avstemmingsnøkkel")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("kvittering.status", "kvittering.beskrivelse")
                it.requireAny("kvittering.status", listOf("OVERFØRT", "FEIL"))
                it.interestedIn("kvittering.feilmelding", "kvittering.xmlmelding")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Forstod ikke kvittering på oppdrag_utbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke kvittering på oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val fagsystemId = packet["fagsystemId"].asText().trim()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val beskrivelse = packet["kvittering.beskrivelse"].asText()
        val status = Oppdragstatus.valueOf(packet["kvittering.status"].asText())

        val oppdrag = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId, fagsystemId)
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
        oppdragDao.oppdaterOppdrag(avstemmingsnøkkel, fagsystemId, status)

        oppdragDao.hentBehovForOppdrag(avstemmingsnøkkel)
            ?.apply {
                this["@løsning"] = mapOf(
                    "Utbetaling" to mapOf(
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
