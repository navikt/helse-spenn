package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class JsonToVedtakParserTest {

    @Test
    fun `les en vilkårlig behandlet søknad`() {
        val soknadId = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadId.toString())
        assertNotNull(vedtak)
        assertEquals(vedtak.aktørId, "1495662136252")
        assertEquals(vedtak.søknadId, soknadId)
        assertEquals(vedtak.vedtaksperioder.size, 1)
        assertEquals(vedtak.vedtaksperioder[0].dagsats, 1154)
        assertEquals(vedtak.vedtaksperioder[0].fom, LocalDate.of(2019, 2, 7))
        assertEquals(vedtak.vedtaksperioder[0].tom, LocalDate.of(2019, 3, 7))
        assertEquals(vedtak.vedtaksperioder[0].fordeling.size, 1)
        assertEquals(vedtak.vedtaksperioder[0].fordeling[0].andel, 100)
        assertEquals(vedtak.vedtaksperioder[0].fordeling[0].mottager, "123412341")
    }

}