package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.system.os.entiteter.oppdragskjema.Attestant
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.oppdragskjema.Grad
import no.nav.system.os.entiteter.oppdragskjema.RefusjonsInfo
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
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

        val utbetalingslinjer = Utbetalingslinjer(
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

    private class Utbetalingslinjer(private val utbetalingsreferanse: String,
                                    private val organisasjonsnummer: String,
                                    private val fødselsnummer: String,
                                    private val forlengelse: Boolean,
                                    private val maksdato: LocalDate) {

        private val nesteId get() = linjer.size + 1
        private val linjer = mutableListOf<Utbetalingslinje>()

        fun isEmpty() = linjer.isEmpty()

        fun førsteDag() = checkNotNull(Utbetalingslinje.førsteDato(linjer)) { "Ingen utbetalingslinjer" }
        fun sisteDag() = checkNotNull(Utbetalingslinje.sisteDato(linjer)) { "Ingen utbetalingslinjer" }

        fun refusjonTilArbeidsgiver(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) {
            linjer.add(
                Utbetalingslinje.RefusjonTilArbeidsgiver(
                    id = nesteId,
                    forlengelse = forlengelse,
                    organisasjonsnummer = organisasjonsnummer,
                    maksdato = maksdato,
                    fom = fom,
                    tom = tom,
                    dagsats = dagsats,
                    grad = grad
                )
            )
        }

        fun utbetalingTilBruker(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) {
            linjer.add(
                Utbetalingslinje.UtbetalingTilBruker(
                    id = nesteId,
                    forlengelse = forlengelse,
                    fødselsnummer = fødselsnummer,
                    fom = fom,
                    tom = tom,
                    dagsats = dagsats,
                    grad = grad
                )
            )
        }

        fun oppdrag(saksbehandler: String) = simFactory.createOppdrag().apply {
            kodeEndring = if (forlengelse) EndringsKode.UENDRET.kode else EndringsKode.NY.kode
            kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
            fagsystemId = utbetalingsreferanse
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            oppdragGjelderId = fødselsnummer
            datoOppdragGjelderFom = LocalDate.EPOCH.format(tidsstempel)
            saksbehId = saksbehandler
            enhet.add(Enhet().apply {
                enhet = OppdragSkjemaConstants.SP_ENHET
                typeEnhet = OppdragSkjemaConstants.BOS
                datoEnhetFom = LocalDate.EPOCH.format(tidsstempel)
            })
            oppdragslinje.addAll(oppdragslinjer(saksbehandler))
        }

        private fun oppdragslinjer(saksbehandler: String): List<Oppdragslinje> = linjer.map { it.somOppdragslinje(saksbehandler) }

        private sealed class Utbetalingslinje(
            private val id: Int,
            private val forlengelse: Boolean,
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val dagsats: Int,
            private val grad: Int
        ) {

            protected abstract fun mottaker(oppdragslinje: Oppdragslinje)

            fun somOppdragslinje(saksbehandler: String): Oppdragslinje {
                return Oppdragslinje().apply {
                    delytelseId = "$id"
                    kodeEndringLinje = if (forlengelse) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
                    kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
                    datoVedtakFom = fom.format(tidsstempel)
                    datoVedtakTom = tom.format(tidsstempel)
                    sats = dagsats.toBigDecimal()
                    fradragTillegg = FradragTillegg.T
                    typeSats = SatsTypeKode.DAGLIG.kode
                    saksbehId = saksbehandler

                    mottaker(this)

                    brukKjoreplan = "N"
                    this.grad.add(Grad().apply {
                        typeGrad = GradTypeKode.UFØREGRAD.kode
                        grad = this@Utbetalingslinje.grad.toBigInteger()
                    })
                    this.attestant.add(Attestant().apply {
                        attestantId = saksbehandler
                    })
                }
            }

            companion object {
                fun førsteDato(linjer: List<Utbetalingslinje>) = linjer.minBy { it.fom }?.fom
                fun sisteDato(linjer: List<Utbetalingslinje>) = linjer.maxBy { it.tom }?.tom
            }

            class RefusjonTilArbeidsgiver(
                id: Int,
                forlengelse: Boolean,
                private val organisasjonsnummer: String,
                private val maksdato: LocalDate,
                fom: LocalDate,
                tom: LocalDate,
                dagsats: Int,
                grad: Int
            ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
                init {
                    require(organisasjonsnummer.length == 9) { "Forventet organisasjonsnummer med lengde 9" }
                }

                override fun mottaker(oppdragslinje: Oppdragslinje) {
                    oppdragslinje.refusjonsInfo = RefusjonsInfo().apply {
                        this.refunderesId = "00$organisasjonsnummer"
                        this.datoFom = oppdragslinje.datoVedtakFom
                        this.maksDato = maksdato.format(tidsstempel)
                    }
                }
            }

            class UtbetalingTilBruker(
                id: Int,
                private val fødselsnummer: String,
                forlengelse: Boolean,
                fom: LocalDate,
                tom: LocalDate,
                dagsats: Int,
                grad: Int
            ) : Utbetalingslinje(id, forlengelse, fom, tom, dagsats, grad) {
                init {
                    require(fødselsnummer.length == 11) { "Forventet fødselsnummer med lengde 11" }
                }

                override fun mottaker(oppdragslinje: Oppdragslinje) {
                    oppdragslinje.utbetalesTilId = fødselsnummer
                }
            }
        }
    }
}
