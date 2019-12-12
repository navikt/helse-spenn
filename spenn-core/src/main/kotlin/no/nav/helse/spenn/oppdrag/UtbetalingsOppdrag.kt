package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.JsonNode
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate


data class UtbetalingsOppdrag(
    val behov: JsonNode,
    /**
     * angir hvem som saken/vedtaket er registrert på i fagrutinen
     */
    val utbetalingsreferanse: String,
    val oppdragGjelder: String,
    val organisasjonsnummer: String,
    val maksdato: LocalDate,
    val saksbehandler: String,

    val utbetalingsLinje: List<UtbetalingsLinje>,
    val annulering:Boolean = false
)

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
