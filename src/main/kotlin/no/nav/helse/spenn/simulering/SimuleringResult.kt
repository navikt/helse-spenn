package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimuleringResult(
    val status: SimuleringStatus,
    val feilmelding: String? = null,
    val simulering: Simulering? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Simulering(
    val gjelderId: String,
    val gjelderNavn: String,
    val datoBeregnet: LocalDate,
    val totalBelop: Int,
    val periodeList: List<SimulertPeriode>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimulertPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetaling: List<Utbetaling>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    val fagSystemId: String,
    val utbetalesTilId: String,
    val utbetalesTilNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<Detaljer>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Detaljer(
    val faktiskFom: LocalDate,
    val faktiskTom: LocalDate,
    val konto: String,
    val belop: Int,
    val tilbakeforing: Boolean,
    val sats: Int,
    val typeSats: String,
    val antallSats: Int,
    val uforegrad: Int,
    val klassekode: String,
    val klassekodeBeskrivelse: String,
    val utbetalingsType: String,
    val refunderesOrgNr: String)


enum class SimuleringStatus {
    OK,
    OPPDRAG_UR_ER_STENGT,
    FUNKSJONELL_FEIL,
    TEKNISK_FEIL
}
