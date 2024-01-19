package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.UtbetalingslinjerMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class Simuleringer(
    rapidsConnection: RapidsConnection,
    private val simuleringV2Service: SimuleringV2Service
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
            validate { it.require("Simulering.maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "Simulering.saksbehandler") }
            validate {
                it.requireKey("Simulering", "Simulering.mottaker", "Simulering.fagsystemId")
                it.requireAny("Simulering.fagområde", listOf("SPREF", "SP"))
                it.requireAny("Simulering.endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("Simulering.linjer") {
                    requireKey("sats", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    requireAny("satstype", listOf("DAG", "ENG"))
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> KodeStatusLinje.valueOf(value.asText()) }
                    interestedIn("grad")
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Fikk et Simulering-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withMDC(
            mapOf(
                "behovId" to packet["@id"].asText()
            )
        ) {
            håndter(packet, context)
        }
    }

    private fun håndter(packet: JsonMessage, context: MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")
        val utbetalingslinjer =
            UtbetalingslinjerMapper(packet["fødselsnummer"].asText(), packet["organisasjonsnummer"].asText())
                .fraBehov(packet["Simulering"])
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")

        try {
            val simulerRequest = SimuleringRequestBuilder(utbetalingslinjer).build()
            val result = simuleringV2Service.simulerOppdrag(simulerRequest)
            packet["@løsning"] = mapOf(
                "Simulering" to mapOf(
                    "status" to result.status,
                    "feilmelding" to result.feilmelding,
                    "simulering" to result.simulering
                )
            )
            context.publish(packet.toJson().also {
                sikkerLogg.info("svarer behov=${packet["@id"].asText()} med $it")
            })
        } catch (err: Exception) {
            log.warn("Ukjent feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
            sikkerLogg.warn("Ukjent feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
        }
    }

    private fun withMDC(context: Map<String, String>, block: () -> Unit) {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        try {
            MDC.setContextMap(contextMap + context)
            block()
        } finally {
            MDC.setContextMap(contextMap)
        }
    }
}
