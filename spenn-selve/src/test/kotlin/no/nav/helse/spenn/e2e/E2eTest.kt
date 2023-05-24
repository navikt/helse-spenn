package no.nav.helse.spenn.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spenn.e2e.E2eTestApp.Companion.e2eTest
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import java.util.UUID.randomUUID

class E2eTest {
    @Test
    fun `happy path`() {
        e2eTest {
            val behov = utbetalingsbehov.json()
            val behovMeldingId = jacksonObjectMapper().readTree(behov).path("@id").asText()
            rapid.sendTestMessage(behov)

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(2, rapid.inspektør.size)

            val oppdragutbetaling = rapid.inspektør.message(1) as ObjectNode
            assertEquals("oppdrag_utbetaling", oppdragutbetaling.path("@event_name").asText())
            oppdragutbetaling.set<JsonNode>("kvittering", jacksonObjectMapper().convertValue<JsonNode>(mapOf(
                "status" to "OVERFØRT",
                "beskrivelse" to "Overført til OS!"
            )))
            rapid.sendTestMessage(oppdragutbetaling.toString())

            assertTrue(erLøsningOverført(2))

            val løsning = parseOkLøsning(rapid.inspektør.message(2))
            val avstemming = løsning.avstemmingsnøkkel

            val transaksjonId = UUID.randomUUID()
            rapid.sendTestMessage(JsonMessage.newMessage("transaksjon_status", mapOf<String, Any>(
                "@id" to transaksjonId,
                "fødselsnummer" to utbetalingsbehov.fnr,
                "avstemmingsnøkkel" to avstemming,
                "utbetalingId" to utbetalingsbehov.utbetalingId,
                "fagsystemId" to utbetalingsbehov.fagsystemId,
                "status" to "AKSEPTERT",
                "feilkode_oppdrag" to "00",
                "beskrivelse" to "Akseptert uten problemer",
                "originalXml" to ""
            )).toJson())

            assertEquals(4, rapid.inspektør.size)
            val finalLøsning = rapid.inspektør.message(3)
            assertEquals("behov", finalLøsning["@event_name"].asText())
            assertEquals("AKSEPTERT", finalLøsning["@løsning"].path("Utbetaling").path("status").asText())

            assertEquals(behovMeldingId, rapid.inspektør.field(0, "@forårsaket_av").path("id").asText())
            assertNotEquals(behovMeldingId, rapid.inspektør.field(0, "@id").asText())
            assertEquals(transaksjonId.toString(), rapid.inspektør.field(3, "@forårsaket_av").path("id").asText())
        }
    }

    @Test
    fun `utbetalinger på samme fnr, utbetalingsid og fagsystemId sendes ikke på nytt til oppdrag etter overført`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseMottattLøsning(rapid.inspektør.message(0))
            val oppdragutbetaling = rapid.inspektør.message(1) as ObjectNode
            assertEquals("oppdrag_utbetaling", oppdragutbetaling.path("@event_name").asText())
            oppdragutbetaling.set<JsonNode>("kvittering", jacksonObjectMapper().convertValue<JsonNode>(mapOf(
                "status" to "OVERFØRT",
                "beskrivelse" to "Overført til OS!"
            )))
            rapid.sendTestMessage(oppdragutbetaling.toString())
            assertEquals(3, rapid.inspektør.size)

            rapid.sendTestMessage(utbetaling1.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(4, rapid.inspektør.size)

            assertEquals(1, sikkerLoggMeldinger().filter { it.startsWith("Mottatt duplikat") }.size)

            assertEquals("MOTTATT", løsning1.status)
            val oppdragutbetalingOpprettet = oppdragutbetaling.path("@opprettet").asLocalDateTime()
            parseOkLøsning(rapid.inspektør.message(2)).also { løsningKopi ->
                assertEquals(løsningKopi.avstemmingsnøkkel, løsning1.avstemmingsnøkkel)
                assertEquals(løsningKopi.overføringstidspunkt?.withNano(0), oppdragutbetalingOpprettet.withNano(0))
                assertEquals("OVERFØRT", løsningKopi.status)
            }
            parseOkLøsning(rapid.inspektør.message(3)).also { løsningKopi ->
                assertEquals(løsningKopi.avstemmingsnøkkel, løsning1.avstemmingsnøkkel)
                assertEquals("OVERFØRT", løsningKopi.status)
            }
        }
    }


    @Test
    fun `utbetalinger på samme fnr og utbetalingsid, men ulik fagsystemId`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("to")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseMottattLøsning(rapid.inspektør.message(0))
            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(2, database.hentAlleOppdrag().size)
            assertEquals(4, rapid.inspektør.size)

            assertEquals(0, sikkerLoggMeldinger().filter { it.startsWith("Motatt duplikat") }.size)

            val løsningKopi = parseMottattLøsning(rapid.inspektør.message(2))
            assertNotEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
            assertEquals("MOTTATT", løsningKopi.status)
        }
    }

    @Test
    fun `utbetalinger på samme fnr og med samme utbetalingsid sender oppdrag på nytt hvis det feilet tidligere`() {
        val utbetaling1 = utbetalingsbehov.fagsystemId("en").utbetalingId(randomUUID())
        val utbetalingKopi = utbetaling1.fagsystemId("en")
        e2eTest {
            rapid.sendTestMessage(utbetaling1.json())
            val løsning1 = parseMottattLøsning(rapid.inspektør.message(0))

            rapid.sendTestMessage(JsonMessage.newMessage("transaksjon_status", mapOf<String, Any>(
                "fødselsnummer" to utbetaling1.fnr,
                "avstemmingsnøkkel" to løsning1.avstemmingsnøkkel,
                "fagsystemId" to utbetaling1.fagsystemId,
                "utbetalingId" to utbetaling1.utbetalingId,
                "status" to "AVVIST",
                "feilkode_oppdrag" to "08",
                "beskrivelse" to "Oppdraget ble avvist",
                "originalXml" to """<?xml version="1.0" encoding="utf-8"?><ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag"></ns2:oppdrag>"""
            )).toJson())

            rapid.sendTestMessage(utbetalingKopi.json())

            assertEquals(1, database.hentAlleOppdrag().size)
            assertEquals(5, rapid.inspektør.size) //behov, løsning på behov, oppdrag_utbetaling, nytt behov, oppdrag_utbetaling

            parseOkLøsning(rapid.inspektør.message(2)).also { løsningKopi ->
                assertEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
                assertEquals("AVVIST", løsningKopi.status)
            }
            parseMottattLøsning(rapid.inspektør.message(3)).also { løsningKopi ->
                assertNotEquals(løsning1.avstemmingsnøkkel, løsningKopi.avstemmingsnøkkel)
                assertEquals("MOTTATT", løsningKopi.status)
            }
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
            assertEquals(4, rapid.inspektør.size)
        }
    }
}