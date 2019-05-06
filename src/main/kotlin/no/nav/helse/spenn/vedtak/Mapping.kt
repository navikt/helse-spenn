package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal

import java.time.LocalDate
import java.util.*

typealias Fodselsnummer = String

interface AktørTilFnrMapper {
    fun tilFnr(aktørId: String): Fodselsnummer
}

private class DummyAktørMapper() : AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer = aktørId
}

fun Vedtak.tilUtbetaling(mapper: AktørTilFnrMapper = DummyAktørMapper()): UtbetalingsOppdrag = UtbetalingsOppdrag(
        vedtak = defaultObjectMapper.convertValue(this, JsonNode::class.java),
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = mapper.tilFnr(aktørId),
        utbetalingsLinje = lagLinjer()
)

private fun Vedtak.lagLinjer(): List<UtbetalingsLinje> =
        vedtaksperioder.mapIndexed { index, periode ->
            periode.fordeling.mapIndexed { fordelingsIndex, fordeling ->
                UtbetalingsLinje(
                        id = "$index/$fordelingsIndex",
                        datoFom = periode.fom,
                        datoTom = periode.tom,
                        sats = BigDecimal(periode.dagsats),
                        satsTypeKode = SatsTypeKode.DAGLIG,
                        utbetalesTil = fordeling.mottager,
                        grad = periode.grad.toBigInteger()
                )
            }
        }.flatten()

fun JsonNode.tilVedtak(key: String): Vedtak =
        Vedtak(søknadId = UUID.fromString(key),
                aktørId = this.get("originalSøknad").get("aktorId").textValue(),
                vedtaksperioder = lagVedtaksperioder(this.get("vedtak").get("perioder")))

private fun lagVedtaksperioder(perioderNode: JsonNode): List<Vedtaksperiode> =
        when (perioderNode) {
            is ArrayNode -> {
                perioderNode.map { pNode ->
                    Vedtaksperiode(
                            fom = pNode.get("fom").asLocalDate(),
                            tom = pNode.get("tom").asLocalDate(),
                            dagsats = pNode.get("dagsats").intValue(),
                            fordeling = lagFordelinger(pNode.get("fordeling"))
                    )
                }
            }
            else -> emptyList()
        }

private fun lagFordelinger(fordelingsNode: JsonNode): List<Fordeling> =
        when (fordelingsNode) {
            is ArrayNode -> {
                fordelingsNode.map { fNode ->
                    Fordeling(
                            mottager = fNode.get("mottager").textValue(),
                            andel = fNode.get("andel").intValue()
                    )
                }
            }
            else -> emptyList()
        }

private fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(textValue())