package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.spenn.SimuleringClient
import com.github.navikt.tbd_libs.spenn.SimuleringClient.SimuleringResult
import com.github.navikt.tbd_libs.spenn.SimuleringRequest
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

internal class Simuleringer(
    rapidsConnection: RapidsConnection,
    private val simuleringClient: SimuleringClient
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Simuleringer::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "behov") }
            precondition { it.requireAll("@behov", listOf("Simulering")) }
            precondition { it.forbid("@løsning") }
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
                    interestedIn("datoKlassifikFom", JsonNode::asLocalDate)
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("grad")
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerLogg.error("Fikk et Simulering-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val callId = UUID.randomUUID().toString()
        withMDC(
            mapOf(
                "behovId" to packet["@id"].asText(),
                "callId" to callId
            )
        ) {
            håndter(packet, callId, context)
        }
    }

    private fun håndter(packet: JsonMessage, callId: String, context: MessageContext) {
        log.info("løser simuleringsbehov id=${packet["@id"].asText()}")
        if (packet["Simulering.linjer"].isEmpty) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")

        try {
            val simulerRequest = SimuleringRequest(
                fødselsnummer = packet["fødselsnummer"].asText(),
                oppdrag = SimuleringRequest.Oppdrag(
                    fagområde = when (val fagområde = packet["Simulering.fagområde"].asText()) {
                        "SP" -> SimuleringRequest.Oppdrag.Fagområde.BRUKERUTBETALING
                        "SPREF" -> SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON
                        else -> error("Forventet ikke fagområde: $fagområde")
                    },
                    fagsystemId = packet["Simulering.fagsystemId"].asText(),
                    endringskode = endringskode(packet["Simulering.endringskode"].asText()),
                    mottakerAvUtbetalingen = packet["Simulering.mottaker"].asText(),
                    linjer = packet["Simulering.linjer"].map { linje ->
                        SimuleringRequest.Oppdrag.Oppdragslinje(
                            endringskode = endringskode(linje.path("endringskode").asText()),
                            fom = linje.path("fom").asLocalDate(),
                            tom = linje.path("tom").asLocalDate(),
                            satstype = when (val type = linje.path("satstype").asText()) {
                                "DAG" -> SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.DAGLIG
                                "ENG" -> SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.ENGANGS
                                else -> error("Forventet ikke satstype: $type")
                            },
                            sats = linje.path("sats").asInt(),
                            grad = linje.path("grad").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
                            delytelseId = linje.path("delytelseId").asInt(),
                            refDelytelseId = linje.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
                            refFagsystemId = linje.path("refFagsystemId").takeUnless(JsonNode::isMissingOrNull)?.asText(),
                            klassekode = when (val kode = linje.path("klassekode").asText()) {
                                "SPATORD" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SYKEPENGER_ARBEIDSTAKER_ORDINÆR
                                "SPATFER" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SYKEPENGER_ARBEIDSTAKER_FERIEPENGER
                                "SPREFAG-IOP" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG
                                "SPREFAGFER-IOP" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_FERIEPENGER_IKKE_OPPLYSNINGSPLIKTIG
                                "SPSND-OP" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SELVSTENDIG_NÆRINGSDRIVENDE
                                "SPSNDFISK" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SELVSTENDIG_NÆRINGSDRIVENDE_FISKER
                                "SPSNDJORD" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SELVSTENDIG_NÆRINGSDRIVENDE_JORDBRUK
                                "SPSNDDM-OP" -> SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.BARNEPASSER
                                else -> error("Forventet ikke klassekode: $kode")
                            },
                            klassekodeFom = linje.path("datoKlassifikFom").asOptionalLocalDate(),
                            opphørerFom = linje.path("datoStatusFom").asOptionalLocalDate()
                        )
                    }
                ),
                maksdato = packet["Simulering.maksdato"].asLocalDate(),
                saksbehandler = packet["Simulering.saksbehandler"].asText()
            )

            when (val result = simuleringClient.hentSimulering(simulerRequest, callId)) {
                is Result.Error -> {
                    sikkerLogg.info("Feil ved simulering: {}", result.error, result.cause)
                    packet["@løsning"] = mapOf(
                        "Simulering" to mapOf(
                            "status" to "TEKNISK_FEIL",
                            "feilmelding" to result.error,
                            "simulering" to null
                        )
                    )
                }
                is Result.Ok -> when (val simuleringresultat = result.value) {
                    is SimuleringResult.Ok -> {
                        packet["@løsning"] = mapOf(
                            "Simulering" to mapOf(
                                "status" to "OK",
                                "feilmelding" to null,
                                "simulering" to simuleringresultat.data
                            )
                        )
                    }
                    SimuleringResult.OkMenTomt -> {
                        packet["@løsning"] = mapOf(
                            "Simulering" to mapOf(
                                "status" to "OK",
                                "feilmelding" to null,
                                "simulering" to null
                            )
                        )
                    }
                    SimuleringResult.SimuleringtjenesteUtilgjengelig -> {
                        sikkerLogg.info("Oppdrag/UR er nede")
                        packet["@løsning"] = mapOf(
                            "Simulering" to mapOf(
                                "status" to "OPPDRAG_UR_ER_STENGT",
                                "feilmelding" to "Oppdrag/UR er stengt",
                                "simulering" to null
                            )
                        )
                    }
                    is SimuleringResult.FunksjonellFeil -> {
                        sikkerLogg.info("Feil ved simulering: {}", simuleringresultat.feilmelding)
                        packet["@løsning"] = mapOf(
                            "Simulering" to mapOf(
                                "status" to "FUNKSJONELL_FEIL",
                                "feilmelding" to simuleringresultat.feilmelding,
                                "simulering" to null
                            )
                        )
                    }
                }
            }

            context.publish(packet.toJson().also {
                sikkerLogg.info("svarer behov=${packet["@id"].asText()} med $it")
            })
        } catch (err: Exception) {
            log.warn("Ukjent feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
            sikkerLogg.warn("Ukjent feil ved simulering for behov=${packet["@id"].asText()}: ${err.message}", err)
        }
    }

    private fun endringskode(kode: String) =
        when (kode) {
            "NY" -> SimuleringRequest.Oppdrag.Endringskode.NY
            "ENDR" -> SimuleringRequest.Oppdrag.Endringskode.ENDRET
            "UEND" -> SimuleringRequest.Oppdrag.Endringskode.IKKE_ENDRET
            else -> error("Forventet ikke endringskode: $kode")
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
