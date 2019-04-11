package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class VedtakParser {

    fun parse(key: String, root: JsonNode): Vedtak =
            Vedtak(søknadId = key,
                    aktørId = root.get("originalSøknad").get("aktorId").textValue(),
                    vedtaksperioder = parsePerioder(root.get("vedtak").get("perioder")))


    private fun parsePerioder(perioderNode: JsonNode): List<Vedtaksperiode> =
            when (perioderNode) {
                is ArrayNode -> {
                    perioderNode.map { pNode ->
                        Vedtaksperiode(
                                fom = pNode.get("fom").asLocalDate(),
                                tom = pNode.get("tom").asLocalDate(),
                                dagsats = pNode.get("dagsats").intValue(),
                                fordeling = parseFordelinger(pNode.get("fordeling"))
                        )
                    }
                }
                else -> emptyList()
            }

    private fun parseFordelinger(fordelingsNode: JsonNode): List<Fordeling> =
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
}

private fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(textValue())
