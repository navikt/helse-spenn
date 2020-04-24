package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.fail

internal class RapidInspektør(private val inspektør: no.nav.helse.rapids_rivers.testsupport.TestRapid.RapidInspector) {
    val size get() = inspektør.size

    fun melding(indeks: Int) = inspektør.message(indeks)
    fun id(indeks: Int) = inspektør.field(indeks, "@id").asText()
    fun løsning(indeks: Int, behov: String, block: (JsonNode) -> Unit = {}) = inspektør.field(indeks, "@løsning").path(behov).takeUnless(
        JsonNode::isMissingOrNull)?.also(block) ?: fail {
        "Behov har ikke løsning for $behov"
    }
}
