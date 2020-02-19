package no.nav.helse.spenn.oppdrag

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate


internal fun Utbetalingsbehov.tilUtbetalingsOppdrag(erLinjeEndring: Boolean = false) =
    UtbetalingsOppdrag(
            behov = this.behov,
            utbetalingsreferanse = this.utbetalingsreferanse,
            oppdragGjelder = this.oppdragGjelder,
            saksbehandler = this.saksbehandler,
            utbetaling = this.utbetaling?.let { behovUtbetaling ->
                Utbetaling(
                        maksdato = behovUtbetaling.maksdato,
                        organisasjonsnummer = behovUtbetaling.organisasjonsnummer,
                        utbetalingsLinjer = behovUtbetaling.utbetalingsLinjer.mapIndexed { i, behovsLinje ->
                            UtbetalingsLinje(
                                    id = "${i + 1}",
                                    satsTypeKode = SatsTypeKode.DAGLIG,
                                    utbetalesTil = behovUtbetaling.organisasjonsnummer,
                                    sats = behovsLinje.sats,
                                    grad = 100.toBigInteger(),
                                    datoFom = behovsLinje.datoFom,
                                    datoTom = behovsLinje.datoTom,
                                    erEndring = erLinjeEndring
                            )
                        }
                )
            }
    )



internal data class UtbetalingsOppdrag(
    val behov: String,
    val utbetalingsreferanse: String,
    val saksbehandler: String,
    /**
     * angir hvem som saken/vedtaket er registrert på i fagrutinen (fnr)
     */
    val oppdragGjelder: String,
    /**
     * utbetaling=NULL betyr aNULLer:
     */
    val utbetaling: Utbetaling?,
    val statusEndringFom: LocalDate? = null,
    val opprinneligOppdragTom: LocalDate? = null
)

internal data class Utbetaling(
    val organisasjonsnummer: String,
    val maksdato: LocalDate,
    val utbetalingsLinjer: List<UtbetalingsLinje>
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
    val grad: BigInteger,
    val erEndring: Boolean? = false
) {
    fun erEndring() = erEndring != null && erEndring
}
