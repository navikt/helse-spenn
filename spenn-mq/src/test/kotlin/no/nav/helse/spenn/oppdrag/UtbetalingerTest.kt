package no.nav.helse.spenn.oppdrag

import io.mockk.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.RapidInspektør
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.oppdrag.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.oppdrag.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingerTest {
    companion object {
        const val PERSON = "12345678911"
        const val ORGNR = "123456789"
        const val BELØP = 1000
        const val FAGSYSTEMID = "838069327ea2"
        const val SAKSBEHANDLER = "Navn Navnesen"
        const val SEND_QUEUE = "utbetalingQueue"
        const val REPLY_TO_QUEUE = "statusQueue"
    }

    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Utbetalinger(this, Jms(connection, SEND_QUEUE, REPLY_TO_QUEUE).sendSession())
    }
    private val inspektør get() = RapidInspektør(rapid.inspektør)

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `løser utbetalingsbehov med engangsutbetaling`() {
        val behov = utbetalingsbehov.linjer(
            utbetalingslinje
                .grad(null)
                .satstype("ENG")
        )
        rapid.sendTestMessage(behov.json())

        assertEquals(1, inspektør.size)
        assertEquals(1, connection.inspektør.antall())
        val body = connection.inspektør.melding(0).getBody(String::class.java)
        val unmarshalled = OppdragXml.unmarshal(body)
        assertEquals(0, unmarshalled.oppdrag110!!.oppdragsLinje150[0].grad170.size)
        assertEquals(SatstypeDto.ENG, unmarshalled.oppdrag110!!.oppdragsLinje150[0].typeSats)

        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertOverført(0)
    }

    @Test
    fun `utbetalingsbehov med exception`() {
        connection.kastExceptionNesteSend()
        rapid.sendTestMessage(utbetalingsbehov.json())
        assertEquals(1, inspektør.size)
        assertEquals(0, connection.inspektør.antall())
        inspektør.løsning(0) {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
    }

    private fun assertOverført(indeks: Int) {
        inspektør.løsning(indeks) {
            assertEquals(Oppdragstatus.OVERFØRT.name, it.path("status").asText())
        }
    }
}
