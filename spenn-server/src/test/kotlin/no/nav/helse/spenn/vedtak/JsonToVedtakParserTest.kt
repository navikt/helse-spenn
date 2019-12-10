package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class JsonToVedtakParserTest {

    @Test
    fun `les en vilkårlig behandlet søknad`() {
        val sakskompleksId = UUID.fromString("e25ccad5-f5d5-4399-bb9d-43e9fc487888")
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        assertNotNull(behov)
        assertEquals(behov.sakskompleksId, sakskompleksId)
        assertEquals(behov.aktørId, "1234567890123")
        assertEquals(behov.utbetalingslinjer.size, 1)
        assertEquals(behov.utbetalingslinjer[0].dagsats, "1000.0".toBigDecimal())
        assertEquals(behov.utbetalingslinjer[0].fom, LocalDate.of(2011, 1, 1))
        assertEquals(behov.utbetalingslinjer[0].tom, LocalDate.of(2011, 1, 31))
        assertEquals(behov.utbetalingslinjer[0].grad, 100)
    }

}