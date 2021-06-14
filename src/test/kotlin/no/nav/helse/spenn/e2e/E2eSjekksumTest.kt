package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.e2e.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID.randomUUID

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
    fun `Eksempel på falsk positiv for sjekksumkollisjon - Utbetalinger med ulike linjer men sjekksumkollisjon skal utbetales`() {
        val fnr = "12345678912"
        val behov = utbetalingsbehov
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
        val behovSomGirSjekksumkollisjon = utbetalingsbehov
            .fnr(fnr)
            .utbetalingId(randomUUID())
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




        e2eTest {
            rapid.sendTestMessage(
                behov
            )

            assertEquals(1, oppdrag.meldinger.size)
            assertEquals(1, rapid.inspektør.size)

            val løsning1 = parseOkLøsning(rapid.inspektør.message(0))
            assertEquals("OVERFØRT", løsning1.status)


            rapid.sendTestMessage(
                behovSomGirSjekksumkollisjon
            )

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)

            val løsning2 = parseOkLøsning(rapid.inspektør.message(1))

            assertEquals("OVERFØRT", løsning2.status)
            assertNotEquals(løsning1.avstemmingsnøkkel, løsning2.avstemmingsnøkkel)

            assertEquals(1, sikkerLoggMeldinger().filter { it.startsWith("Sjekksumkollisjon for fnr") }.size)
        }
    }
}
