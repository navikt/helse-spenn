package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.Utbetaling
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.time.LocalDate

typealias Fodselsnummer = String

internal object SpennOppdragFactory {
    fun lagOppdragFraBehov(jsonNode : JsonNode, fodselsnummer : String) : UtbetalingsOppdrag {

        fun organisasjonsnummer() = jsonNode["organisasjonsnummer"].asText()!!
        return UtbetalingsOppdrag(
            behov = jsonNode,
            utbetalingsreferanse = jsonNode["utbetalingsreferanse"].asText()!!,
            oppdragGjelder = fodselsnummer,
            saksbehandler = jsonNode["saksbehandler"].asText()!!,
            utbetaling = jsonNode["utbetalingslinjer"]?.let { utbetalingslinjer ->
                Utbetaling(
                    maksdato = jsonNode["maksdato"].asText()!!.let { LocalDate.parse(it) },
                    organisasjonsnummer = organisasjonsnummer(),
                    utbetalingsLinjer = utbetalingslinjer.mapIndexed { i, behovsLinje ->
                        UtbetalingsLinje(
                            id = "${i + 1}",
                            satsTypeKode = SatsTypeKode.DAGLIG,
                            utbetalesTil = organisasjonsnummer(),
                            sats = BigDecimal(behovsLinje["dagsats"].asText()!!),
                            grad = 100.toBigInteger(), //BigInteger.valueOf(behovsLinje["grad"].asLong()),
                            datoFom = behovsLinje["fom"].asText()!!.let { LocalDate.parse(it) },
                            datoTom = behovsLinje["tom"].asText()!!.let { LocalDate.parse(it) }
                        )
                    }
                )
            }
        )
    }
}
