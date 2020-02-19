package no.nav.helse.spenn.oppdrag

import no.nav.helse.rapids_rivers.JsonMessage
import java.math.BigDecimal
import java.time.LocalDate

class Utbetalingsbehov(jsonMessage: JsonMessage) {
    internal val behov = jsonMessage.toString()
    val utbetalingsreferanse = jsonMessage["utbetalingsreferanse"].asText()!!
    internal val oppdragGjelder = jsonMessage["fødselsnummer"].asText()
    internal val saksbehandler = jsonMessage["saksbehandler"].asText()!!
    internal val utbetaling = jsonMessage["utbetalingslinjer"]?.let { utbetalingslinjer ->
        UtbetalingsbehovUtbetaling(
                maksdato = jsonMessage["maksdato"].asText()!!.let { LocalDate.parse(it) },
                organisasjonsnummer = jsonMessage["organisasjonsnummer"].asText()!!,
                utbetalingsLinjer = utbetalingslinjer.mapIndexed { i, behovsLinje ->
                    UtbetalingsbehovLinje(
                            /*id = "${i + 1}",
                                    satsTypeKode = SatsTypeKode.DAGLIG,*/
                            utbetalesTil = jsonMessage["organisasjonsnummer"].asText()!!,
                            sats = BigDecimal(behovsLinje["dagsats"].asText()!!),
                            //grad = 100.toBigInteger(), //BigInteger.valueOf(behovsLinje["grad"].asLong()),
                            datoFom = behovsLinje["fom"].asText()!!.let { LocalDate.parse(it) },
                            datoTom = behovsLinje["tom"].asText()!!.let { LocalDate.parse(it) }
                    )
                }
        )
    }
}

internal class UtbetalingsbehovUtbetaling(
        val organisasjonsnummer: String,
        val maksdato: LocalDate,
        val utbetalingsLinjer: List<UtbetalingsbehovLinje>
)

internal class UtbetalingsbehovLinje(
        val sats: BigDecimal,
        val datoFom: LocalDate,
        val datoTom: LocalDate,
        /**
         * Kan registreres med fødselsnummer eller organisasjonsnummer til den enheten som skal motta ubetalingen. Normalt
         * vil dette være den samme som oppdraget gjelder, men kan f.eks være en arbeidsgiver som skal få refundert pengene.
         */
        val utbetalesTil: String
)