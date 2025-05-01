package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.util.*
import org.slf4j.LoggerFactory

internal class Feriepenger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Feriepenger::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("Feriepengeutbetaling"))
                it.forbid("@løsning")
            }
            validate {
                it.requireKey("@id", "fødselsnummer", "organisasjonsnummer")
                it.requireKey(
                    "Feriepengeutbetaling",
                    "Feriepengeutbetaling.saksbehandler",
                    "Feriepengeutbetaling.mottaker",
                    "Feriepengeutbetaling.fagsystemId",
                    "utbetalingId"
                )
                it.requireAny("Feriepengeutbetaling.fagområde", listOf("SPREF", "SP"))
                it.requireAny("Feriepengeutbetaling.endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("Feriepengeutbetaling.linjer") {
                    requireKey("sats", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    requireValue("satstype", "ENG")
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> check(value.asText() in setOf("OPPH")) }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Forstod ikke behov om Feriepengeutbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke behov om Feriepengeutbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        log.info("løser feriepengeutbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["Feriepengeutbetaling.mottaker"].asText()
        val fagområde = packet["Feriepengeutbetaling.fagområde"].asText()
        val fagsystemId = packet["Feriepengeutbetaling.fagsystemId"].asText().trim()
        val endringskode = packet["Feriepengeutbetaling.endringskode"].asText()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val saksbehandler = packet["Feriepengeutbetaling.saksbehandler"].asText()

        håndterUtbetalingsbehov(
            behovnavn = "Feriepengeutbetaling",
            context = context,
            oppdragDao = oppdragDao,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            endringskode = endringskode,
            utbetalingId = utbetalingId,
            saksbehandler = saksbehandler,
            maksdato = null,
            linjer = packet["Feriepengeutbetaling.linjer"],
            packet = packet
        )
    }
}
