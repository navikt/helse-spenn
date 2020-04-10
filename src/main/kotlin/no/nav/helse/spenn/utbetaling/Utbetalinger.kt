package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.ibm.mq.jms.MQQueue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import javax.jms.Connection

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    jmsConnection: Connection,
    sendQueue: String,
    private val replyTo: String,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Utbetalinger::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val jmsSession = jmsConnection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.requireContains("@behov", "Utbetaling") }
            validate { it.forbid("@løsning") }
            validate { it.require("maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "saksbehandler") }
            validate {
                it.requireKey("utbetalingsreferanse", "sjekksum")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("linjertype", listOf("NY", "UEND", "ENDR"))
                it.requireArray("linjer") {
                    requireKey("dagsats", "grad", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("linjetype", listOf("NY", "UEND", "ENDR"))
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val utbetalingsreferanse = packet["utbetalingsreferanse"].asText()
        val utbetalingslinjer = UtbetalingslinjerMapper.fraBehov(packet)
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        val nå = Instant.now()
        val tidspunkt = nå
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)
        val oppdrag = OppdragBuilder(utbetalingslinjer, avstemmingsnøkkel, nå).build()

        if (!oppdragDao.nyttOppdrag(packet["fagområde"].asText(), avstemmingsnøkkel, utbetalingslinjer.sjekksum, fødselsnummer, tidspunkt, utbetalingsreferanse,
                Oppdragstatus.OVERFØRT, utbetalingslinjer.totalbeløp(), packet.toJson())) {
            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag: har samme avstemmingsnøkkel eller sjekksum"
                )
            )
        } else {
            sendOppdrag(oppdrag)
            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.OVERFØRT,
                    "overføringstidspunkt" to tidspunkt,
                    "avstemmingsnøkkel" to avstemmingsnøkkel
                )
            )
        }

        context.send(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
    }

    private fun sendOppdrag(oppdrag: Oppdrag) {
        val oppdragXml = OppdragXml.marshal(oppdrag)
        val message = jmsSession.createTextMessage(oppdragXml)
        message.jmsReplyTo = MQQueue(replyTo)
        producer.send(message)
    }
}