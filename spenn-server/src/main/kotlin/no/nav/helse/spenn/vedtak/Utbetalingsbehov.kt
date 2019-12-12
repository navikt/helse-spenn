package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

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
            operasjon = AksjonsKode.OPPDATER,
            saksbehandler = jsonNode["saksbehandler"].asText()!!
        )
    }
}

/*
class Utbetalingsbehov(private val jsonNode) {
    val sakskompleksId: UUID,
    val utbetalingsreferanse: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val maksdato: LocalDate,
    val saksbehandler: String,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val annulering: Boolean = false,
    val løsning: Løsning? = null

    fun tilUtbetaling(fodselsnummer: Fodselsnummer): UtbetalingsOppdrag {
        if (this.annulering) require(utbetalingslinjer.isEmpty())
        return UtbetalingsOppdrag(
            behov = this,
            operasjon = AksjonsKode.OPPDATER,
            oppdragGjelder = fodselsnummer,
            annulering = annulering,
            utbetalingsLinje = lagLinjer(),
            utbetalingsreferanse = utbetalingsreferanse,
            organisasjonsnummer = organisasjonsnummer,
            maksdato = maksdato,
            saksbehandler = saksbehandler
        )
    }

    private fun lagLinjer(): List<UtbetalingsLinje> =
        utbetalingslinjer.mapIndexed { index, periode ->
            UtbetalingsLinje(
                id = (index + 1).toString(),
                datoFom = periode.fom,
                datoTom = periode.tom,
                sats = periode.dagsats,
                satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = organisasjonsnummer,
                grad = 100.toBigInteger() //periode.grad.toBigInteger()
            )
        }
}

data class Løsning(val avstemmingsnokkel: String)

data class Utbetalingslinje(
    val grad: Int,
    val dagsats: BigDecimal,
    val fom: LocalDate,
    val tom: LocalDate
)
*/