package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimuleringResult(val status: Status,
                            val feilMelding: String = "",
                            val mottaker: Mottaker? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(val gjelderId: String,
                    val gjelderNavn: String,
                    val datoBeregnet: String,
                    val totalBelop: BigDecimal,
                    val kodeFaggruppe: String = "",
                    val periodeList: List<Periode>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Periode(val id: String,
                   val faktiskFom: LocalDate,
                   val faktiskTom: LocalDate,
                   val oppdragsId: Long,
                   val forfall: LocalDate,
                   val utbetalesTilId: String,
                   val utbetalesTilNavn: String,
                   val konto: String,
                   val belop: BigDecimal,
                   val sats: BigDecimal,
                   val typeSats: SatsTypeKode,
                   val antallSats: BigDecimal,
                   val uforegrad: BigInteger,
                   val utbetalingsType: UtbetalingsType,
                   val tilbakeforing: Boolean,
                   val behandlingsKode: String = "")

enum class Status {
    OK,
    FEIL
}