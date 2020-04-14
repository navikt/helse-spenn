package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.TestRapid
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingerTest {
    private companion object {
        private const val FAGOMRÅDE_REFUSJON = "SPREF"
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val SJEKKSUM = -873852214
        private const val BELØP = 1000
        private const val FAGSYSTEMID = "838069327ea2"
        private const val BEHOV = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val SEND_QUEUE = "utbetalingQueue"
        private const val REPLY_TO_QUEUE = "statusQueue"
    }

    private val dao = mockk<OppdragDao>()
    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Utbetalinger(this, connection,
            SEND_QUEUE,
            REPLY_TO_QUEUE, dao)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `løser utbetalingsbehov`() {
        every { dao.nyttOppdrag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true
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
        verify(exactly = 0) { dao.nyttOppdrag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `utbetalingsbehov med feil`() {
        every { dao.nyttOppdrag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns false
        rapid.sendTestMessage(utbetalingsbehov())
        assertEquals(1, rapid.inspektør.antall())
        assertEquals(0, connection.inspektør.antall())
        assertEquals(BEHOV, rapid.inspektør.id(0))
        rapid.inspektør.løsning(0, "Utbetaling") {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
        verify(exactly = 1) { dao.nyttOppdrag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `utbetalingsbehov med exception`() {
        every { dao.nyttOppdrag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException()
        rapid.sendTestMessage(utbetalingsbehov())
        assertEquals(1, rapid.inspektør.antall())
        assertEquals(0, connection.inspektør.antall())
        assertEquals(BEHOV, rapid.inspektør.id(0))
        rapid.inspektør.løsning(0, "Utbetaling") {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
    }

    private fun assertOverført(indeks: Int) {
        assertEquals(BEHOV, rapid.inspektør.id(indeks))
        rapid.inspektør.løsning(indeks, "Utbetaling") {
            assertEquals(Oppdragstatus.OVERFØRT.name, it.path("status").asText())
            assertDoesNotThrow { it.path("avstemmingsnøkkel").asText().toLong() }
            assertDoesNotThrow { LocalDateTime.parse(it.path("overføringstidspunkt").asText()) }
        }
        val avstemmingsnøkkel = rapid.inspektør.løsning(indeks, "Utbetaling")
            .path("avstemmingsnøkkel")
            .asLong()
        verify(exactly = 1) { dao.nyttOppdrag(
            FAGOMRÅDE_REFUSJON, avstemmingsnøkkel,
            SJEKKSUM,
            PERSON,
            ORGNR,
            any(),
            FAGSYSTEMID,
            Oppdragstatus.OVERFØRT,
            BELØP,
            any()) }
    }

    private fun utbetalingsbehov(utbetalingslinjer: List<Map<String, Any>> = listOf(
        mapOf(
            "dagsats" to "$BELØP",
            "fom" to "2020-04-20",
            "tom" to "2020-05-20",
            "grad" to 100
        )
    )): String {
        return jacksonObjectMapper().writeValueAsString(
            mapOf(
                "@event_name" to "behov",
                "@behov" to listOf("Utbetaling"),
                "@id" to BEHOV,
                "organisasjonsnummer" to ORGNR,
                "mottaker" to ORGNR,
                "fødselsnummer" to PERSON,
                "saksbehandler" to SAKSBEHANDLER,
                "maksdato" to "2020-04-20",
                "mottaker" to ORGNR,
                "fagområde" to "SPREF",
                "fagsystemId" to FAGSYSTEMID,
                "endringskode" to "NY",
                "sjekksum" to SJEKKSUM,
                "linjer" to utbetalingslinjer.map {
                    mapOf<String, Any?>(
                        "fom" to it["fom"],
                        "tom" to it["tom"],
                        "dagsats" to it["dagsats"],
                        "grad" to it["grad"],
                        "delytelseId" to 1,
                        "refDelytelseId" to null,
                        "refFagsystemId" to null,
                        "endringskode" to "NY",
                        "klassekode" to "SPREFAG-IOP"
                    )
                }
            )
        )
    }

}
