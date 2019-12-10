package no.nav.helse.spenn.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import no.nav.helse.spenn.vedtak.Utbetalingslinje
import java.time.LocalDate
import java.util.*

internal class TestData {

    companion object {
        fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
            val node = ObjectMapper().readTree(this::class.java.getResource("/et_utbetalingsbehov.json"))
            val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
            return behov.tilUtbetaling("01010112345")
        }
    }
}

fun etEnkeltBehov(
    sakskompleksId: UUID = UUID.randomUUID(),
    maksdato: LocalDate = LocalDate.now().plusYears(1)
) = Utbetalingsbehov(
    sakskompleksId = sakskompleksId,
    utbetalingsreferanse = "1001",
    aktørId = "en random aktørid",
    saksbehandler = "yes",
    organisasjonsnummer = "897654321",
    utbetalingslinjer = listOf(
        Utbetalingslinje(
            fom = LocalDate.of(2020, 1, 15),
            tom = LocalDate.of(2020, 1, 30),
            dagsats = 1234.toBigDecimal(),
            grad = 100
        )
    ),
    maksdato = maksdato
)