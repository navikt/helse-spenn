package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

typealias Fodselsnummer = String

internal object SpennOppdragFactory {
    fun lagOppdragFraBehov(jsonNode : JsonNode, fodselsnummer : String) : UtbetalingsOppdrag {
        val organisasjonsnummer = jsonNode["organisasjonsnummer"].asText()!!
        return UtbetalingsOppdrag(
            behov = jsonNode,
            utbetalingsreferanse = jsonNode["utbetalingsreferanse"].asText()!!,
            maksdato = jsonNode["maksdato"].asText()!!.let { LocalDate.parse(it) },
            oppdragGjelder = fodselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingsLinje = jsonNode["utbetalingslinjer"].mapIndexed { i, behovsLinje ->
                UtbetalingsLinje(
                    id = "${i + 1}",
                    satsTypeKode = SatsTypeKode.DAGLIG,
                    utbetalesTil = organisasjonsnummer,
                    sats = BigDecimal(behovsLinje["dagsats"].asText()!!),
                    grad = BigInteger.valueOf(behovsLinje["grad"].asLong()),
                    datoFom = behovsLinje["fom"].asText()!!.let { LocalDate.parse(it) },
                    datoTom = behovsLinje["tom"].asText()!!.let { LocalDate.parse(it) }
                )
            },
            saksbehandler = jsonNode["saksbehandler"].asText()!!
        )
    }
}
