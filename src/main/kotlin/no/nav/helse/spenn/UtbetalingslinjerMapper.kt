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
            mottaker = packet["mottaker"].asText(),
            fagsystemId = packet["fagsystemId"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            endringskode = packet["endringskode"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet["maksdato"].takeUnless(JsonNode::isMissingOrNull)?.asLocalDate()
        )

    private fun utbetalingTilBruker(packet: JsonMessage) =
        Utbetalingslinjer.UtbetalingTilBruker(
            fagsystemId = packet["fagsystemId"].asText(),
            fødselsnummer = packet["fødselsnummer"].asText(),
            mottaker = packet["mottaker"].asText(),
            organisasjonsnummer = packet["organisasjonsnummer"].asText(),
            endringskode = packet["endringskode"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet["maksdato"].takeUnless(JsonNode::isMissingOrNull)?.asLocalDate()
        )

    private fun mapLinjer(utbetalingslinjer: Utbetalingslinjer, linjer: JsonNode) {
        linjer.forEach {
            Utbetalingslinjer.Utbetalingslinje(
                delytelseId = it["delytelseId"].asInt(),
                endringskode = it["endringskode"].asText(),
                klassekode = it["klassekode"].asText(),
                fom = it["fom"].asLocalDate(),
                tom = it["tom"].asLocalDate(),
                dagsats = it["dagsats"].asInt(),
                grad = it["grad"].asInt(),
                refDelytelseId = it.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
                refFagsystemId = it.path("refFagsystemId").takeUnless(JsonNode::isMissingOrNull)?.asText(),
                datoStatusFom = it.path("datoStatusFom").takeUnless(JsonNode::isMissingOrNull)?.asLocalDate(),
                statuskode = it.path("statuskode").takeUnless(JsonNode::isMissingOrNull)?.asText()
            ).also { utbetalingslinjer.linje(it) }
        }
    }
}
