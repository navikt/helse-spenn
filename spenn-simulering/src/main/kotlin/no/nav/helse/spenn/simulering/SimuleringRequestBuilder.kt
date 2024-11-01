package no.nav.helse.spenn.simulering

import java.time.LocalDate

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
