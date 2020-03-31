package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.ObjectFactory as GrensesnittObjectFactory
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest as SimulerSPBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.ObjectFactory as SimuleringObjectFactory

internal class Simuleringløser(
    rapidsConnection: RapidsConnection,
    private val simuleringService: SimuleringService
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Simuleringløser::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val simFactory = SimuleringObjectFactory()
        private val grensesnittFactory = GrensesnittObjectFactory()
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.requireAll("@behov", listOf("Simulering")) }
            validate { it.forbid("@løsning") }
            validate { it.require("maksdato", JsonNode::asLocalDate) }
            validate { it.requireKey("@id", "fødselsnummer", "utbetalingsreferanse",
                "utbetalingslinjer", "organisasjonsnummer", "forlengelse") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")

        val utbetalingslinjer = Oppdragslinjer(
            utbetalingsreferanse = packet["utbetalingsreferanse"].asText(),
            organisasjonsnummer = packet["organisasjonsnummer"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            forlengelse = packet["forlengelse"].asBoolean(),
            maksdato = packet["maksdato"].asLocalDate()
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

        val oppdrag = utbetalingslinjer.oppdrag(OppdragSkjemaConstants.APP)

        val request = simuleringRequest(oppdrag, utbetalingslinjer.førsteDag(), utbetalingslinjer.sisteDag())
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

    private fun simuleringRequest(oppdrag: Oppdrag, fom: LocalDate, tom: LocalDate): SimulerSPBeregningRequest {
        return grensesnittFactory.createSimulerBeregningRequest().apply {
            this.request = simFactory.createSimulerBeregningRequest().apply {
                this.oppdrag = oppdrag
                simuleringsPeriode = SimulerBeregningRequest.SimuleringsPeriode().apply {
                    datoSimulerFom = fom.format(tidsstempel)
                    datoSimulerTom = tom.format(tidsstempel)
                }
            }
        }
    }

}
