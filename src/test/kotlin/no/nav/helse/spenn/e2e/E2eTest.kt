package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Kvittering.Companion.kvittering
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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
    fun `utbetalinger på samme fnr, utbetalingsid og fagsystemId sendes ikke på nytt til oppdrag etter overført`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseOkLøsning(rapid.inspektør.message(0))

            rapid.sendTestMessage(utbetaling1.json())

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
    fun `utbetalinger på samme fnr og utbetalingsid, men ulik fagsystemId`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseOkLøsning(rapid.inspektør.message(0))

            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)

            assertEquals(0, sikkerLoggMeldinger().filter { it.startsWith("Motatt duplikat") }.size)

            val løsningKopi = parseOkLøsning(rapid.inspektør.message(1))
            assertNotEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
            assertNotEquals(løsning1.overføringstidspunkt, løsningKopi.overføringstidspunkt)
            assertEquals("OVERFØRT", løsningKopi.status)
        }
    }

    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sender oppdrag på nytt hvis det feilet tidligere`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("en")
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
}