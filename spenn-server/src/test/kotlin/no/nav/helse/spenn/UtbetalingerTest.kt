package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class UtbetalingerTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val BEHOV = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val SEND_QUEUE = "utbetalingQueue"
        private const val REPLY_TO_QUEUE = "statusQueue"
    }

    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Utbetalinger(this, connection, SEND_QUEUE, REPLY_TO_QUEUE)
    }

    @BeforeEach
    fun clear() {
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `løser utbetalingsbehov`() {
        rapid.sendTestMessage(utbetalingsbehov())
        assertEquals(1, rapid.inspektør.antall())
        assertEquals(1, connection.inspektør.antall())
        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertOverført(0)
    }

    @Test
    fun `ignorerer tomme utbetalingslinjer`() {
        rapid.sendTestMessage(utbetalingsbehov(emptyList()))
        assertEquals(0, rapid.inspektør.antall())
        assertEquals(0, connection.inspektør.antall())
    }

    private fun assertOverført(indeks: Int) {
        assertEquals(BEHOV, rapid.inspektør.id(indeks))
        rapid.inspektør.løsning(indeks, "Utbetaling") {
            assertEquals(Oppdragstatus.OVERFØRT.name, it.path("status").asText())
            assertDoesNotThrow { it.path("avstemmingsnøkkel").asText().toLong() }
            assertDoesNotThrow { ZonedDateTime.parse(it.path("overføringstidspunkt").asText()) }
        }
    }

    private fun utbetalingsbehov(utbetalingslinjer: List<Map<String, Any>> = listOf(
        mapOf(
            "dagsats" to "1000.0",
            "fom" to "2020-04-20",
            "tom" to "2020-05-20",
            "grad" to 100
        )
    )): String {
        return defaultObjectMapper.writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Utbetaling"),
                "@id" to BEHOV,
                "organisasjonsnummer" to ORGNR,
                "fødselsnummer" to PERSON,
                "saksbehandler" to SAKSBEHANDLER,
                "maksdato" to "2020-04-20",
                "utbetalingsreferanse" to "ref",
                "forlengelse" to false,
                "utbetalingslinjer" to utbetalingslinjer
            )
        )
    }

}
