package no.nav.helse.spenn.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.vedtak.Utbetalingsbehov

internal class TestData {

    companion object {
        fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
            val node = ObjectMapper().readTree(this::class.java.getResource("/et_utbetalingsbehov.json"))
            val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
            return behov.tilUtbetaling("01010112345")
        }
    }

}