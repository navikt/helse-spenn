package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate


@JsonIgnoreProperties(ignoreUnknown = true)
data class UtbetalingsOppdrag(
    val behov: Utbetalingsbehov,
    val operasjon: AksjonsKode,
    /**
     * angir hvem som saken/vedtaket er registrert på i fagrutinen
     */
    val oppdragGjelder: String,
    val utbetalingsLinje: List<UtbetalingsLinje>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtbetalingsLinje(
    /**
     * delytelseId - fagsystemets entydige identifikasjon av oppdragslinjen
     */
    val id: String,
    val sats: BigDecimal,
    val satsTypeKode: SatsTypeKode,
    val datoFom: LocalDate,
    val datoTom: LocalDate,
    /**
     * Kan registreres med fødselsnummer eller organisasjonsnummer til den enheten som skal motta ubetalingen. Normalt
     * vil dette være den samme som oppdraget gjelder, men kan f.eks være en arbeidsgiver som skal få refundert pengene.
     */
    val utbetalesTil: String,
    val grad: BigInteger
)
