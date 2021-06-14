package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Kvittering.Companion.kvittering
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.e2e.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID.randomUUID

class E2eTest {
    @Test
    fun `happy path`() {
        e2eTest {
            rapid.sendTestMessage(utbetalingsbehov.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)
            assertEquals(1, rapid.inspektør.size)

            assertTrue(erLøsningOverført(0))

            val løsning = parseOkLøsning(rapid.inspektør.message(0))
            val avstemming = løsning.avstemmingsnøkkel

            oppdrag.meldingFraOppdrag(
                kvittering
                    .fagsystemId(utbetalingsbehov.fagsystemId)
                    .avstemmingsnøkkel(avstemming)
                    .akseptert()
                    .toXml()
            )
            assertEquals(3, rapid.inspektør.size)
            assertEquals("oppdrag_kvittering", rapid.inspektør.message(1)["@event_name"].asText())

            val status = rapid.inspektør.message(2)
            assertEquals("transaksjon_status", status["@event_name"].asText())
            assertEquals("AKSEPTERT", status["status"].asText())
        }
    }


    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sendes ikke på nytt til oppdrag etter overført`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseOkLøsning(rapid.inspektør.message(0))

            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)

            assertEquals(1, sikkerLoggMeldinger().filter { it.startsWith("Motatt duplikat") }.size)

            val løsningKopi = parseOkLøsning(rapid.inspektør.message(1))
            assertEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
            assertNotEquals(løsning1.overføringstidspunkt, løsningKopi.overføringstidspunkt)
            assertEquals("OVERFØRT", løsningKopi.status)
        }
    }

    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sender oppdrag på nytt hvis det feilet tidligere`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseOkLøsning(rapid.inspektør.message(0))

            oppdrag.meldingFraOppdrag(
                kvittering
                    .fagsystemId(utbetaling1.fagsystemId)
                    .avstemmingsnøkkel(løsning1.avstemmingsnøkkel)
                    .funksjonellFeil()
                    .toXml()
            )

            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(4, rapid.inspektør.size) //behov, transaksjonsstatus, kvittering, nytt behov

            val løsningKopi = parseOkLøsning(rapid.inspektør.message(3))
            assertEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
            assertNotEquals(løsning1.overføringstidspunkt, løsningKopi.overføringstidspunkt)
            assertEquals("OVERFØRT", løsningKopi.status)
        }
    }

    @Test
    fun `utbetalinger med samme utbetalingid men ulik fnr fungerer uavhengig av hverandre`() {
        val utbetaling1 = utbetalingsbehov.fnr("12345678901").utbetalingId(randomUUID())
        val utbetaling2 = utbetalingsbehov.fnr("10987654321").utbetalingId(utbetaling1.utbetalingId)
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())

            rapid.sendTestMessage(utbetaling2.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)
        }
    }


    @Test
    fun `Linjer hvor bare fagsystemid er forskjellig er en indikator på dobbelt utbetaling, skal utbetales men logges`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en")
        val utbetaling2 = utbetaling1
            .fagsystemId("to")
            .utbetalingId(randomUUID())

        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            rapid.sendTestMessage(utbetaling2.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size) //behov, transaksjonsstatus, kvittering, nytt behov

            assertEquals(1, sikkerLoggMeldinger().filter { it.startsWith("Sjekksumkollisjon for fnr") }.size)

            val løsningFraUtbetaling2 = parseOkLøsning(rapid.inspektør.message(1))
            assertEquals("OVERFØRT", løsningFraUtbetaling2.status)
        }
    }

    @Test
    fun `Linjer hvor bare fagsystemid er forskjellig er en indikator på dobbelt utbetaling, skal utbetales men logges- flere linjer`() {
        val utbetaling1 = utbetalingsbehov
            .linjer(
                utbetalingslinje,
                utbetalingslinje
                    .grad(20)
                    .sats(100)
                    .fom(utbetalingslinje.tom.plusDays(1))
                    .tom(utbetalingslinje.tom.plusDays(2))
            )
            .fagsystemId("en")
        val utbetaling2 = utbetaling1
            .fagsystemId("to")
            .utbetalingId(randomUUID())

        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            rapid.sendTestMessage(utbetaling2.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size) //behov, transaksjonsstatus, kvittering, nytt behov

            assertEquals(1, sikkerLoggMeldinger().filter { it.startsWith("Sjekksumkollisjon for fnr") }.size)

            val løsningFraUtbetaling2 = parseOkLøsning(rapid.inspektør.message(1))
            assertEquals("OVERFØRT", løsningFraUtbetaling2.status)
        }
    }

    @Test
    fun `Hvis bare noen linjer er like skal meldingen slippes gjennom`() {
        val utbetaling1 = utbetalingsbehov
            .linjer(
                utbetalingslinje,
                utbetalingslinje
                    .grad(20)
                    .sats(100)
                    .fom(utbetalingslinje.tom.plusDays(1))
                    .tom(utbetalingslinje.tom.plusDays(2))
            )
            .fagsystemId("en")
        val utbetaling2 = utbetaling1
            .linjer(
                utbetalingslinje,
                utbetalingslinje
                    .tom(LocalDate.now())
            )
            .fagsystemId("to")
            .utbetalingId(randomUUID())

        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            rapid.sendTestMessage(utbetaling2.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size) //behov, transaksjonsstatus, kvittering, nytt behov

            val løsningFraUtbetaling2 = parseOkLøsning(rapid.inspektør.message(1))
            assertEquals("OVERFØRT", løsningFraUtbetaling2.status)
        }
    }
}