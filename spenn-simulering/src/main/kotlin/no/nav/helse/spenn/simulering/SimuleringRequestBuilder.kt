package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.Utbetalingslinjer
import java.time.LocalDate

internal class SimuleringRequestBuilder(private val utbetalingslinjer: Utbetalingslinjer) {
    private val oppdragslinjer = mutableListOf<Oppdragslinje>()

    private val oppdrag = Oppdrag(
        kodeFagomraade = utbetalingslinjer.fagområde,
        kodeEndring = utbetalingslinjer.endringskode,
        utbetFrekvens = "MND",
        fagsystemId = utbetalingslinjer.fagsystemId,
        oppdragGjelderId = utbetalingslinjer.fødselsnummer,
        saksbehId = utbetalingslinjer.saksbehandler,
        datoOppdragGjelderFom = LocalDate.EPOCH,
        enhet = listOf(Enhet(
            enhet = "8020",
            typeEnhet = "BOS",
            datoEnhetFom = LocalDate.EPOCH
        )),
        oppdragslinje = oppdragslinjer
    )

    private val linjeStrategy: (Utbetalingslinjer.Utbetalingslinje) -> Oppdragslinje = when (utbetalingslinjer) {
        is Utbetalingslinjer.RefusjonTilArbeidsgiver -> ::refusjonTilArbeidsgiver
        is Utbetalingslinjer.UtbetalingTilBruker -> ::utbetalingTilBruker
    }

    fun build(): SimulerBeregningRequest {
        utbetalingslinjer.forEach { oppdragslinjer.add(linjeStrategy(it)) }
        return SimulerBeregningRequest(
            oppdrag = this@SimuleringRequestBuilder.oppdrag,
            simuleringsPeriode = SimuleringsPeriode(
                datoSimulerFom = utbetalingslinjer.førsteDag(),
                datoSimulerTom = utbetalingslinjer.sisteDag()
            )
        )
    }

    private fun refusjonTilArbeidsgiver(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) =
        nyLinje(utbetalingslinje).apply {
            refusjonsInfo = RefusjonsInfo(
                refunderesId = utbetalingslinjer.mottaker.padStart(11, '0'),
                datoFom = datoVedtakFom,
                maksDato = utbetalingslinjer.maksdato?.takeUnless { it == LocalDate.MAX }
            )
        }

    private fun utbetalingTilBruker(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) =
        nyLinje(utbetalingslinje).apply {
            utbetalesTilId = utbetalingslinjer.mottaker
        }

    private fun nyLinje(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) = Oppdragslinje(
        delytelseId = "${utbetalingslinje.delytelseId}",
        refDelytelseId = utbetalingslinje.refDelytelseId?.let { "$it" },
        refFagsystemId = utbetalingslinje.refFagsystemId,
        kodeEndringLinje = utbetalingslinje.endringskode,
        kodeKlassifik = utbetalingslinje.klassekode,
        kodeStatusLinje = utbetalingslinje.statuskode?.let { KodeStatusLinje.valueOf(it) },
        datoStatusFom = utbetalingslinje.datoStatusFom,
        datoVedtakFom = utbetalingslinje.fom,
        datoVedtakTom = utbetalingslinje.tom,
        sats = utbetalingslinje.sats,
        fradragTillegg = FradragTillegg.T,
        typeSats = utbetalingslinje.satstype,
        saksbehId = utbetalingslinjer.saksbehandler,
        brukKjoreplan = "N",
        grad = listOf(Grad(
            typeGrad = "UFOR",
            grad = utbetalingslinje.grad
        )),
        attestant = listOf(Attestant(
            attestantId = utbetalingslinjer.saksbehandler
        ))
    )
}

data class SimulerBeregningRequest(val oppdrag: Oppdrag, val simuleringsPeriode: SimuleringsPeriode)
data class Oppdrag(
    val kodeFagomraade: String,
    val kodeEndring: String,
    val utbetFrekvens: String,
    val fagsystemId: String,
    val oppdragGjelderId: String,
    val saksbehId: String,
    val datoOppdragGjelderFom: LocalDate?,
    val enhet: List<Enhet>,
    val oppdragslinje: MutableList<Oppdragslinje>
)
data class Enhet(val enhet: String, val typeEnhet: String, val datoEnhetFom: LocalDate?)
data class SimuleringsPeriode(val datoSimulerFom: LocalDate, val datoSimulerTom: LocalDate)
data class RefusjonsInfo(val refunderesId: String, val datoFom: LocalDate, val maksDato: LocalDate?)
data class Oppdragslinje(
    val delytelseId: String,
    val refDelytelseId: String?,
    val refFagsystemId: String?,
    val kodeEndringLinje: String,
    val kodeKlassifik: String,
    val kodeStatusLinje: KodeStatusLinje?,
    val datoStatusFom: LocalDate?,
    val datoVedtakFom: LocalDate,
    val datoVedtakTom: LocalDate,
    val sats: Int,
    val fradragTillegg: FradragTillegg,
    val typeSats: String,
    val saksbehId: String,
    val brukKjoreplan: String,
    val grad: List<Grad>,
    val attestant: List<Attestant>
) {
    var refusjonsInfo: RefusjonsInfo? = null
    var utbetalesTilId: String? = null
}

enum class FradragTillegg {
    F, T
}
enum class KodeStatusLinje {
    OPPH,
    HVIL,
    SPER,
    REAK;
}
data class Grad(val typeGrad: String, val grad: Int?)
data class Attestant(val attestantId: String)
