package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.ibm.mq.jms.MQQueue
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
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
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAll("@behov", listOf("Utbetaling")) }
            validate { it.rejectKey("@løsning") }
            validate { it.interestedIn("maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "saksbehandler") }
            validate {
                it.requireKey("mottaker", "fagsystemId")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("linjer") {
                    requireKey("dagsats", "grad", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> TkodeStatusLinje.valueOf(value.asText()) }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("Fikk et Utbetaling-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["mottaker"].asText()
        val fagsystemId = packet["fagsystemId"].asText()
        val utbetalingslinjer = UtbetalingslinjerMapper.fraBehov(packet)
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        val nå = Instant.now()
        val tidspunkt = nå
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)
        val oppdrag = OppdragBuilder(utbetalingslinjer, avstemmingsnøkkel, nå).build()
        val sjekksum = utbetalingslinjer.hashCode()

        try {
            if (!oppdragDao.nyttOppdrag(
                    fagområde = packet["fagområde"].asText(),
                    avstemmingsnøkkel = avstemmingsnøkkel,
                    fødselsnummer = fødselsnummer,
                    sjekksum = sjekksum,
                    organisasjonsnummer = organisasjonsnummer,
                    mottaker = mottaker,
                    tidspunkt = tidspunkt,
                    fagsystemId = fagsystemId,
                    status = Oppdragstatus.OVERFØRT,
                    totalbeløp = utbetalingslinjer.totalbeløp(),
                    originalJson = packet.toJson()
                )) {
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
                        "beskrivelse" to "Oppdraget er sendt til Oppdrag/UR. Venter på kvittering",
                        "overføringstidspunkt" to tidspunkt,
                        "avstemmingsnøkkel" to avstemmingsnøkkel
                    )
                )
            }
        } catch (err: Exception) {
            log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil"
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
