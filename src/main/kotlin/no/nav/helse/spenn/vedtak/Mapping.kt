package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.spenn.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import java.math.BigDecimal

import java.time.LocalDate
import java.util.*


typealias Fodselsnummer = String


fun Vedtak.tilUtbetaling(fodselsnummer: Fodselsnummer): UtbetalingsOppdrag = UtbetalingsOppdrag(
        vedtak = this, //defaultObjectMapper.convertValue(this, JsonNode::class.java),
        operasjon = AksjonsKode.OPPDATER,
        oppdragGjelder = fodselsnummer,
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
        Vedtak(soknadId = UUID.fromString(key),
                aktorId = this.get("originalSøknad").get("aktorId").textValue(),
                vedtaksperioder = lagVedtaksperioder(this.get("vedtak").get("perioder")),
                maksDato = this.get("avklarteVerdier").get("maksdato").get("fastsattVerdi").asLocalDate()
                )

private fun lagVedtaksperioder(perioderNode: JsonNode?): List<Vedtaksperiode> =
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