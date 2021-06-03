package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class E2eTest {
    @Test
    fun `happy path`() {
        e2eTest {
            rapid.sendTestMessage(utbetalingsbehov.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(1, this.oppdrag.meldinger.size)
            assertEquals(1, rapid.inspektør.size)

            val behovsvar = rapid.inspektør.message(0)
            val løsning = behovsvar["@løsning"]["Utbetaling"]
            val avstemming = løsning["avstemmingsnøkkel"].asLong()
            assertEquals("OVERFØRT", løsning["status"].asText())

            oppdrag.meldingFraOppdrag(
                Kvittering(
                    fagsystemId = utbetalingsbehov.fagsystemid,
                    avstemmingsnøkkel = avstemming
                ).toXml()
            )
            assertEquals("oppdrag_kvittering", rapid.inspektør.message(1)["@event_name"].asText())

            val status = rapid.inspektør.message(2)
            assertEquals("transaksjon_status", status["@event_name"].asText())
            assertEquals("AKSEPTERT", status["status"].asText())
        }
    }
}