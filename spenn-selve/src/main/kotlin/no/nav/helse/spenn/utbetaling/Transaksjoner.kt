package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Transaksjoner(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Transaksjoner::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "transaksjon_status") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate {
                it.requireKey(
                    "@id", "fødselsnummer", "fagsystemId", "utbetalingId",
                    "avstemmingsnøkkel", "feilkode_oppdrag", "beskrivelse", "originalXml"
                )
            }
            validate { it.requireAny("status", Oppdragstatus.values().map(Enum<*>::name)) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Forstod ikke transaksjon_status (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke transaksjon_status:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val fagsystemId = packet["fagsystemId"].asText()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val status = Oppdragstatus.valueOf(packet["status"].asText())
        val tidspunkt = packet["@opprettet"].asLocalDateTime()
        log.info("oppdrag med utbetalingId=$utbetalingId avstemmingsnøkkel=${avstemmingsnøkkel} status=${status} tidspunkt=$tidspunkt")
        sikkerLogg.info("oppdrag med utbetalingId=$utbetalingId avstemmingsnøkkel=${avstemmingsnøkkel} status=${status} tidspunkt=$tidspunkt")

        if (!oppdragDao.oppdaterOppdrag(utbetalingId, fagsystemId, status, packet["beskrivelse"].asText(), packet["feilkode_oppdrag"].asText(), packet["originalXml"].asText())) {
            log.error("Klarte ikke å oppdatere oppdrag i databasen! utbetalingId=$utbetalingId fagsystemId=$fagsystemId status=$status")
            sikkerLogg.error("Klarte ikke å oppdatere oppdrag i databasen! utbetalingId=$utbetalingId fagsystemId=$fagsystemId fødselsnummer=$fødselsnummer status=$status :\n${packet.toJson()}")
            return
        }

        oppdragDao.hentBehovForOppdrag(utbetalingId, fagsystemId)?.also {
            it["@id"] = UUID.randomUUID()
            it["@opprettet"] = LocalDateTime.now()
            sikkerLogg.info(
                "oppdrag med utbetalingId=$utbetalingId avstemmingsnøkkel=$avstemmingsnøkkel fagsystemId=$fagsystemId " +
                        "fødselsnummer=$fødselsnummer status=$status tidspunkt=$tidspunkt for behov=${it.toJson()}"
            )

            it["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to status,
                    "overføringstidspunkt" to tidspunkt,
                    "avstemmingsnøkkel" to avstemmingsnøkkel,
                    "beskrivelse" to packet["beskrivelse"].asText()
                )
            )
            context.publish(it.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        }
    }
}
