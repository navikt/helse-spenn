package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spenn.OppdragSkjemaConstants
import no.nav.helse.spenn.Utbetalingslinjer
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

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
            validate { it.requireValue("@event_name", "behov") }
            validate { it.requireContains("@behov", "Simulering") }
            validate { it.forbid("@løsning") }
            validate { it.require("maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "utbetalingsreferanse", "organisasjonsnummer", "forlengelse") }
            validate { it.requireArray("utbetalingslinjer") {
                requireKey("fom", "tom", "dagsats", "grad")
            } }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")
        val utbetalingslinjer = Utbetalingslinjer(
            utbetalingsreferanse = packet["utbetalingsreferanse"].asText(),
            organisasjonsnummer = packet["organisasjonsnummer"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            forlengelse = packet["forlengelse"].asBoolean()
        ).apply {
            packet["utbetalingslinjer"].forEach {
                refusjonTilArbeidsgiver(
                    fom = it["fom"].asLocalDate(),
                    tom = it["tom"].asLocalDate(),
                    dagsats = it["dagsats"].asInt(),
                    grad = it["grad"].asDouble().roundToInt()
                )
            }
        }

        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")

        val request = SimuleringRequestBuilder(
            saksbehandler = OppdragSkjemaConstants.APP,
            maksdato = packet["maksdato"].asLocalDate(),
            utbetalingslinjer = utbetalingslinjer
        ).build()

        simuleringService.simulerOppdrag(request).also { result ->
            packet["@løsning"] = mapOf(
                "Simulering" to mapOf(
                    "status" to result.status,
                    "feilmelding" to result.feilMelding,
                    "simulering" to result.simulering
                )
            )
            context.send(packet.toJson().also {
                sikkerLogg.info("svarer behov=${packet["@id"].asText()} med $it")
            })
        }
    }
}
