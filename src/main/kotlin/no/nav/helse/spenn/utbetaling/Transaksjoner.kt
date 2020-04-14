package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

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
            validate { it.requireValue("@event_name", "transaksjon_status") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey("@id", "fødselsnummer", "fagsystemId",
                "avstemmingsnøkkel", "feilkode_oppdrag", "beskrivelse", "originalXml") }
            validate { it.requireAny("status", Oppdragstatus.values().map(Enum<*>::name)) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val fagsystemId = packet["fagsystemId"].asText()
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val status = Oppdragstatus.valueOf(packet["status"].asText())
        val tidspunkt = packet["@opprettet"].asLocalDateTime()
        log.info("oppdrag med avstemmingsnøkkel=${avstemmingsnøkkel} status=${status} tidspunkt=$tidspunkt")

        oppdragDao.hentBehovForOppdrag(avstemmingsnøkkel)?.also {
            sikkerLogg.info("oppdrag med avstemmingsnøkkel=$avstemmingsnøkkel fagsystemId=$fagsystemId " +
                    "fødselsnummer=$fødselsnummer status=$status tidspunkt=$tidspunkt for behov=${it.toJson()}")

            it["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to status,
                    "overføringstidspunkt" to tidspunkt,
                    "avstemmingsnøkkel" to avstemmingsnøkkel,
                    "beskrivelse" to packet["beskrivelse"].asText()
                )
            )
            context.send(it.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        }
    }
}
