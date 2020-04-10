package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull

internal object UtbetalingslinjerMapper {
    fun fraBehov(packet: JsonMessage): Utbetalingslinjer {
        val fagområde = packet["fagområde"].asText()
        return when (fagområde) {
            "SPREF" -> refusjonTilArbeidsgiver(packet)
            "SP" -> utbetalingTilBruker(packet)
            else -> throw IllegalArgumentException("ukjent fagområde $fagområde")
        }.apply {
            mapLinjer(this, packet["linjer"])
        }
    }

    private fun refusjonTilArbeidsgiver(packet: JsonMessage) =
        Utbetalingslinjer.RefusjonTilArbeidsgiver(
            organisasjonsnummer = packet["organisasjonsnummer"].asText(),
            utbetalingsreferanse = packet["utbetalingsreferanse"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            endringskode = packet["linjertype"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet["maksdato"].asLocalDate(),
            sjekksum = packet["sjekksum"].asInt()
        )

    private fun utbetalingTilBruker(packet: JsonMessage) =
        Utbetalingslinjer.UtbetalingTilBruker(
            utbetalingsreferanse = packet["utbetalingsreferanse"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            endringskode = packet["linjertype"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet["maksdato"].asLocalDate(),
            sjekksum = packet["sjekksum"].asInt()
        )

    private fun mapLinjer(utbetalingslinjer: Utbetalingslinjer, linjer: JsonNode) {
        linjer.forEach {
            Utbetalingslinjer.Utbetalingslinje(
                delytelseId = it["delytelseId"].asInt(),
                endringskode = it["linjetype"].asText(),
                klassekode = it["klassekode"].asText(),
                fom = it["fom"].asLocalDate(),
                tom = it["tom"].asLocalDate(),
                dagsats = it["dagsats"].asInt(),
                grad = it["grad"].asInt(),
                refDelytelseId = it.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt()
            ).also { utbetalingslinjer.linje(it) }
        }
    }
}
