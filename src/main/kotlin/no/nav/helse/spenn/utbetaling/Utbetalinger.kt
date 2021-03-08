package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.ibm.mq.MQException
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.jms.DetailedJMSException
import com.ibm.msg.client.wmq.common.internal.Reason
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import javax.jms.Connection
import javax.jms.JMSException

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
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAll("@behov", listOf("Utbetaling"))
                it.rejectKey("@løsning")
                it.requireKey("@id", "fødselsnummer", "organisasjonsnummer")
                it.interestedIn("Utbetaling.maksdato", JsonNode::asLocalDate)
                it.requireKey("Utbetaling", "Utbetaling.saksbehandler", "Utbetaling.mottaker", "Utbetaling.fagsystemId")
                it.requireAny("Utbetaling.fagområde", listOf("SPREF", "SP"))
                it.requireAny("Utbetaling.endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("Utbetaling.linjer") {
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

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Fikk et Utbetaling-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["Utbetaling.mottaker"].asText()
        val fagsystemId = packet["Utbetaling.fagsystemId"].asText()
        val utbetalingslinjer = UtbetalingslinjerMapper(packet["fødselsnummer"].asText(), packet["organisasjonsnummer"].asText())
                .fraBehov(packet["Utbetaling"])
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        val nå = Instant.now()
        val tidspunkt = nå
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)
        val oppdrag = OppdragBuilder(utbetalingslinjer, avstemmingsnøkkel, nå).build()
        val sjekksum = utbetalingslinjer.hashCode()

        try {
            val oppdragDto = oppdragDao.nyttOppdrag(
                fagområde = packet["Utbetaling.fagområde"].asText(),
                avstemmingsnøkkel = avstemmingsnøkkel,
                fødselsnummer = fødselsnummer,
                sjekksum = sjekksum,
                organisasjonsnummer = organisasjonsnummer,
                mottaker = mottaker,
                tidspunkt = tidspunkt,
                fagsystemId = fagsystemId,
                status = Oppdragstatus.MOTTATT,
                totalbeløp = utbetalingslinjer.totalbeløp(),
                originalJson = packet.toJson()
            ) ?: oppdragDao.hentOppdragForSjekksum(sjekksum)

            oppdragDto?.sendOppdrag(oppdragDao, oppdrag, jmsSession, producer, MQQueue(replyTo))

            packet["@løsning"] = mapOf(
                "Utbetaling" to (oppdragDto?.somLøsning() ?: mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag: har samme avstemmingsnøkkel eller sjekksum"
                ))
            )
        } catch (err: Exception) {
            log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
            packet["@løsning"] = mapOf(
                    "Utbetaling" to mapOf(
                            "status" to Oppdragstatus.FEIL,
                            "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil"
                    )
            )

            // kast exception videre oppover; dersom MQ er nede ønsker vi at spenn skal restarte
            if (err is MQErNede) throw err
        } finally {
            context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        }
    }
}
