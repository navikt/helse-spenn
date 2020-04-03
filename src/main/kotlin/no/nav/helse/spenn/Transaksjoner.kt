package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.simulering.SimuleringService
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

internal class Transaksjoner(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Transaksjoner::class.java)
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "transaksjon_status") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey("@id", "fødselsnummer", "utbetalingsreferanse",
                "avstemmingsnøkkel", "feilkode_oppdrag", "beskrivelse", "originalXml") }
            validate { it.requireAny("status", Oppdragstatus.values().map(Enum<*>::name)) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val status = Oppdragstatus.valueOf(packet["status"].asText())
        val tidspunkt = packet["@opprettet"].asLocalDateTime()
        log.info("oppdrag med avstemmingsnøkkel=${avstemmingsnøkkel} status=${status} tidspunkt=$tidspunkt")

        oppdragDao.hentBehovForOppdrag(avstemmingsnøkkel)?.also {
            it["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to status,
                    "overføringstidspunkt" to tidspunkt,
                    "avstemmingsnøkkel" to avstemmingsnøkkel,
                    "beskrivelse" to packet["beskrivelse"].asText()
                )
            )
            context.send(it.toJson())
        }
    }
}
