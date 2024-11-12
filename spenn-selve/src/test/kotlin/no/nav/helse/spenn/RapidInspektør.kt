package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.junit.jupiter.api.fail

internal class RapidInspektør(private val inspektør: TestRapid.RapidInspector) {
    val size get() = inspektør.size

    fun melding(indeks: Int) = inspektør.message(indeks)
    fun id(indeks: Int) = inspektør.field(indeks, "@id").asText()
    fun behovId(indeks: Int) = inspektør.field(indeks, "@behovId").asText()
    fun løsning(indeks: Int, behov: String, block: (JsonNode) -> Unit = {}) =
        inspektør.field(indeks, "@løsning").path(behov).takeUnless(
            JsonNode::isMissingOrNull
        )?.also(block) ?: fail {
            "Behov har ikke løsning for $behov"
        }
}
