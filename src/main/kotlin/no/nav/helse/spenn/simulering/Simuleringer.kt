package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.UtbetalingslinjerMapper
import org.slf4j.LoggerFactory

internal class Simuleringer(
    rapidsConnection: RapidsConnection,
    private val simuleringService: SimuleringService
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Simuleringer::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAll("@behov", listOf("Simulering")) }
            validate { it.rejectKey("@løsning") }
            validate { it.require("maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "saksbehandler") }
            validate {
                it.requireKey("mottaker", "fagsystemId", "sjekksum")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("linjer") {
                    requireKey("dagsats", "grad", "delytelseId", "klassekode", "datoStatusFom", "statuskode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("Fikk et Simulering-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")
        val utbetalingslinjer = UtbetalingslinjerMapper.fraBehov(packet)
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        simuleringService.simulerOppdrag(SimuleringRequestBuilder(utbetalingslinjer).build()).also { result ->
            packet["@løsning"] = mapOf(
                "Simulering" to mapOf(
                    "status" to result.status,
                    "feilmelding" to result.feilmelding,
                    "simulering" to result.simulering
                )
            )
            context.send(packet.toJson().also {
                sikkerLogg.info("svarer behov=${packet["@id"].asText()} med $it")
            })
        }
    }
}
