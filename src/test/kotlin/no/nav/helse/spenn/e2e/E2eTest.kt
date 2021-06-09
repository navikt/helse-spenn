package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Kvittering.Companion.kvittering
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

            val løsning = okLøsning(0)
            val avstemming = løsning.avstemmingsnøkkel

            oppdrag.meldingFraOppdrag(
                Kvittering(
                    fagsystemId = utbetalingsbehov.fagsystemId,
                    avstemmingsnøkkel = avstemming
                ).toXml()
            )
            assertEquals("oppdrag_kvittering", rapid.inspektør.message(1)["@event_name"].asText())

            val status = rapid.inspektør.message(2)
            assertEquals("transaksjon_status", status["@event_name"].asText())
            assertEquals("AKSEPTERT", status["status"].asText())
        }
    }

    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sendes ikke på nytt til oppdrag etter aksept`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = okLøsning(0)

            oppdrag.meldingFraOppdrag(
                kvittering
                    .fagsystemId(utbetaling1.fagsystemId)
                    .avstemmingsnøkkel(løsning1.avstemmingsnøkkel!!)
                    .akseptert()
                    .toXml()
            )

            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)

            val løsningKopi = okLøsning(1)
            assertNotEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
            assertNotEquals(løsning1.overføringstidspunkt, løsningKopi.overføringstidspunkt)
            assertEquals("OVERFØRT", løsningKopi.status)
        }
    }

    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sender på oppdrag på nytt hvis det feilet tidligere`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = okLøsning(0)

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
            assertEquals(2, rapid.inspektør.size)

            val løsningKopi = okLøsning(1)
            assertNotEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
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
            val løsning1 = okLøsning(0)

            rapid.sendTestMessage(utbetaling2.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(2, this.oppdrag.meldinger.size)
            assertEquals(2, rapid.inspektør.size)
        }
    }
}