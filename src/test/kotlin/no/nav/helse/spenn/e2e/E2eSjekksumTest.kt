package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.e2e.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class E2eSjekksumTest {

    @Test
    fun `Unik kombinasjon av FNR og utbetalingsid overføres til oppdrag`(){
        val fnr = "12345678912"
        e2eTest {
            rapid.sendTestMessage(
                utbetalingsbehov
                    .fnr(fnr)
                    .linjer(
                        utbetalingslinje.copy(
                            sats = 266,
                            grad = 25,
                            fom = LocalDate.parse("2021-01-19"),
                            tom = LocalDate.parse("2021-02-15")
                        )
                    )
                    .json()
            )


            assertEquals(1, oppdrag.meldinger.size)
            assertEquals(1, rapid.inspektør.size)

            val behovsvar1 = rapid.inspektør.message(0)
            assertEquals("266", behovsvar1["Utbetaling"]["linjer"][0]["sats"].asText())

            val løsning1 = behovsvar1["@løsning"]["Utbetaling"]
            assertEquals("OVERFØRT", løsning1["status"].asText())
            val avstemming1 = løsning1["avstemmingsnøkkel"].asText()

            rapid.sendTestMessage(
                utbetalingsbehov
                    .fnr(fnr)
                    .linjer(
                        utbetalingslinje.copy(
                            sats = 213,
                            grad = 20,
                            fom = LocalDate.parse("2021-02-16"),
                            tom = LocalDate.parse("2021-02-28"),
                            delytelseId = 2
                        ),
                    )
                    .json()
            )

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)

            assertEquals(2, rapid.inspektør.size)

            val behovSvar2 = rapid.inspektør.message(1)
            val løsning2 = behovSvar2["@løsning"]["Utbetaling"]
            assertEquals("213", behovSvar2["Utbetaling"]["linjer"][0]["sats"].asText())

            assertEquals("OVERFØRT", løsning2["status"].asText())
            assertEquals(avstemming1, løsning2["avstemmingsnøkkel"].asText())
        }

    }

    @Test
    fun `ved kollisjon for sjekksum og fnr vil andre pakke rapporteres som sendt uten å overføres til oppdrag`() {
        val fnr = "12345678912"
        e2eTest {
            rapid.sendTestMessage(
                utbetalingsbehov
                    .fnr(fnr)
                    .linjer(
                        utbetalingslinje.copy(
                            sats = 266,
                            grad = 25,
                            fom = LocalDate.parse("2021-01-19"),
                            tom = LocalDate.parse("2021-02-15")
                        )
                    )
                    .json()
            )


            assertEquals(1, oppdrag.meldinger.size)
            assertEquals(1, rapid.inspektør.size)

            val behovsvar1 = rapid.inspektør.message(0)
            assertEquals("266", behovsvar1["Utbetaling"]["linjer"][0]["sats"].asText())

            val løsning1 = behovsvar1["@løsning"]["Utbetaling"]
            assertEquals("OVERFØRT", løsning1["status"].asText())
            val avstemming1 = løsning1["avstemmingsnøkkel"].asText()

            rapid.sendTestMessage(
                utbetalingsbehov
                    .fnr(fnr)
                    .linjer(
                        utbetalingslinje.copy(
                            sats = 213,
                            grad = 20,
                            fom = LocalDate.parse("2021-02-16"),
                            tom = LocalDate.parse("2021-02-28"),
                            delytelseId = 2
                        ),
                    )
                    .json()
            )

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)

            assertEquals(2, rapid.inspektør.size)

            val behovSvar2 = rapid.inspektør.message(1)
            val løsning2 = behovSvar2["@løsning"]["Utbetaling"]
            assertEquals("213", behovSvar2["Utbetaling"]["linjer"][0]["sats"].asText())

            assertEquals("OVERFØRT", løsning2["status"].asText())
            assertEquals(avstemming1, løsning2["avstemmingsnøkkel"].asText())
        }
    }
}
