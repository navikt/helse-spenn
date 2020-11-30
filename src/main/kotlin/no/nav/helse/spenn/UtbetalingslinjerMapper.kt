package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull

internal class UtbetalingslinjerMapper(
        private val fødselsnummer: String,
        private val organisasjonsnummer: String
) {
    fun fraBehov(packet: JsonNode): Utbetalingslinjer {
        val fagområde = packet["fagområde"].asText()
        return when (fagområde) {
            "SPREF" -> refusjonTilArbeidsgiver(packet)
            "SP" -> utbetalingTilBruker(packet)
            else -> throw IllegalArgumentException("ukjent fagområde $fagområde")
        }.apply {
            mapLinjer(this, packet["linjer"])
        }
    }

    private fun refusjonTilArbeidsgiver(packet: JsonNode) =
        Utbetalingslinjer.RefusjonTilArbeidsgiver(
            mottaker = packet["mottaker"].asText(),
            fagsystemId = packet["fagsystemId"].asText(),
            fødselsnummer = fødselsnummer,
            endringskode = packet["endringskode"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet.path("maksdato").takeUnless(JsonNode::isMissingOrNull)?.asLocalDate()
        )

    private fun utbetalingTilBruker(packet: JsonNode) =
        Utbetalingslinjer.UtbetalingTilBruker(
            fagsystemId = packet["fagsystemId"].asText(),
            fødselsnummer = fødselsnummer,
            mottaker = packet["mottaker"].asText(),
            organisasjonsnummer = organisasjonsnummer,
            endringskode = packet["endringskode"].asText(),
            saksbehandler = packet["saksbehandler"].asText(),
            maksdato = packet.path("maksdato").takeUnless(JsonNode::isMissingOrNull)?.asLocalDate()
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
