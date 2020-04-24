package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.RapidInspektør
import no.nav.helse.spenn.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class TransaksjonerTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val BELØP = 1000
        private const val FAGSYSTEMID = "838069327ea2"
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val BEHOV_ID = "12c1f3de-c880-41bb-b82a-f339d9f796fc"
        private const val TRANSAKSJON_ID = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val BESKRIVELSE = "Foo"
        private val OPPRETTET = LocalDateTime.now()
        private val STATUS = Oppdragstatus.AKSEPTERT

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val dao = mockk<OppdragDao>()
    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Transaksjoner(this, dao)
    }
    private val inspektør get() = RapidInspektør(rapid.inspektør)

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `løser utbetalingsbehov`() {
        val behov = utbetalingsbehov()
        every { dao.hentBehovForOppdrag(any()) } returns JsonMessage(behov, MessageProblems(behov))
        rapid.sendTestMessage(tranaksjonStatus())
        assertEquals(1, inspektør.size)
        assertEquals("behov", rapid.inspektør.field(0, "@event_name").asText())
        assertEquals(listOf("Utbetaling"), rapid.inspektør.field(0, "@behov").map(JsonNode::asText))
        assertEquals(BEHOV_ID, rapid.inspektør.field(0, "@id").asText())
        assertEquals(ORGNR, rapid.inspektør.field(0, "organisasjonsnummer").asText())
        assertEquals(PERSON, rapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals(SAKSBEHANDLER, rapid.inspektør.field(0, "saksbehandler").asText())
        assertEquals("2020-04-20", rapid.inspektør.field(0, "maksdato").asText())
        assertEquals(FAGSYSTEMID, rapid.inspektør.field(0, "fagsystemId").asText())
        inspektør.løsning(0, "Utbetaling") {
            assertEquals(STATUS.name, it.path("status").asText())
            assertEquals(OPPRETTET, it.path("overføringstidspunkt").asLocalDateTime())
            assertEquals(AVSTEMMINGSNØKKEL, it.path("avstemmingsnøkkel").asLong())
            assertEquals(BESKRIVELSE, it.path("beskrivelse").asText())
        }
    }

    private fun utbetalingsbehov(): String {
        return objectMapper.writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Utbetaling"),
                "@id" to BEHOV_ID,
                "organisasjonsnummer" to ORGNR,
                "fødselsnummer" to PERSON,
                "saksbehandler" to SAKSBEHANDLER,
                "maksdato" to "2020-04-20",
                "fagsystemId" to FAGSYSTEMID,
                "forlengelse" to false,
                "utbetalingslinjer" to listOf(
                    mapOf(
                        "dagsats" to "$BELØP",
                        "fom" to "2020-04-20",
                        "tom" to "2020-05-20",
                        "grad" to 100
                    )
                )
            )
        )
    }

    private fun tranaksjonStatus(): String {
        return objectMapper.writeValueAsString(
            mapOf(
                "@event_name" to "transaksjon_status",
                "@id" to TRANSAKSJON_ID,
                "@opprettet" to OPPRETTET,
                "fødselsnummer" to PERSON,
                "avstemmingsnøkkel" to AVSTEMMINGSNØKKEL,
                "fagsystemId" to FAGSYSTEMID,
                "status" to STATUS,
                "feilkode_oppdrag" to "00",
                "beskrivelse" to BESKRIVELSE,
                "originalXml" to "xml"
            )
        )
    }
}
