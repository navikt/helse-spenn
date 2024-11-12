package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.junit.jupiter.api.fail

internal class RapidInspektør(private val inspektør: TestRapid.RapidInspector) {
    val size get() = inspektør.size

    fun melding(indeks: Int) = inspektør.message(indeks)
    fun id(indeks: Int) = inspektør.field(indeks, "@id").asText()
    fun løsning(indeks: Int, block: (JsonNode) -> Unit = {}) =
        inspektør.field(indeks, "kvittering").takeUnless(JsonNode::isMissingOrNull)?.also(block) ?: fail {
            "utbetalingen har ikke en kvittering"
        }
}
