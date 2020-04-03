package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.spenn.SatsTypeKode
import no.nav.helse.spenn.UtbetalingsType
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimuleringResult(
    val status: SimuleringStatus,
    val feilMelding: String = "",
    val simulering: Simulering? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Simulering(
    val gjelderId: String,
    val gjelderNavn: String,
    val datoBeregnet: LocalDate,
    val totalBelop: BigDecimal,
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
    val belop: BigDecimal,
    val tilbakeforing: Boolean,
    val sats: BigDecimal,
    val typeSats: SatsTypeKode,
    val antallSats: BigDecimal,
    val uforegrad: BigInteger,
    val klassekode: String,
    val klassekodeBeskrivelse: String,
    val utbetalingsType: UtbetalingsType,
    val refunderesOrgNr: String)


enum class SimuleringStatus {
    OK,
    FEIL
}
