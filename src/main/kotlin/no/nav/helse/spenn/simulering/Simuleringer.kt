package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.nav.helse.spenn.UtenforÅpningstidException
import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje
import org.slf4j.LoggerFactory
import org.slf4j.MDC

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
                it.requireKey("mottaker", "fagsystemId")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("linjer") {
                    requireKey("dagsats", "grad", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> KodeStatusLinje.valueOf(value.asText()) }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("Fikk et Simulering-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        withMDC(mapOf(
            "behovId" to packet["@id"].asText()
        )) {
            håndter(packet, context)
        }
    }

    private fun håndter(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")
        val utbetalingslinjer = UtbetalingslinjerMapper.fraBehov(packet)
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")

        try {
            simuleringService.simulerOppdrag(SimuleringRequestBuilder(utbetalingslinjer).build()).also { result ->
                packet["@løsning"] = mapOf(
                    "Simulering" to mapOf(
                        "status" to result.status,
                        "feilmelding" to result.feilmelding,
                        "simulering" to result.simulering
                    )
                )
            }
        } catch (err: UtenforÅpningstidException) {
            packet["@løsning"] = mapOf(
                "Simulering" to mapOf(
                    "status" to SimuleringStatus.OPPDRAG_UR_ER_STENGT,
                    "feilmelding" to "Oppdrag/UR er stengt",
                    "simulering" to null
                )
            )
        } catch (err: Exception) {
            log.error("Teknisk feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
            sikkerLogg.error("Teknisk feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
            packet["@løsning"] = mapOf(
                "Simulering" to mapOf(
                    "status" to SimuleringStatus.TEKNISK_FEIL,
                    "feilmelding" to "Fikk teknisk feil ved simulering",
                    "simulering" to null
                )
            )
        } finally {
            context.send(packet.toJson().also {
                sikkerLogg.info("svarer behov=${packet["@id"].asText()} med $it")
            })
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
