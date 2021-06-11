package no.nav.helse.spenn.utbetaling

import io.mockk.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.Jms
import no.nav.helse.spenn.RapidInspektør
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.e2e.Utbetalingsbehov.Companion.utbetalingsbehov
import no.nav.helse.spenn.e2e.Utbetalingslinje.Companion.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingerTest {
    companion object {
        const val PERSON = "12345678911"
        const val ORGNR = "123456789"
        const val BELØP = 1000
        const val FAGSYSTEMID = "838069327ea2"
        const val BEHOV = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        const val SAKSBEHANDLER = "Navn Navnesen"
        const val SEND_QUEUE = "utbetalingQueue"
        const val REPLY_TO_QUEUE = "statusQueue"
    }

    private val dao = mockk<OppdragDao>()
    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Utbetalinger(
            this, dao, Jms(
                connection,
                SEND_QUEUE,
                REPLY_TO_QUEUE
            ).sendSession()
        )
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
        val avstemmingsnøkkel = CapturingSlot<Long>()
        val behov = utbetalingsbehov
        every {
            dao.nyttOppdrag(
                any(),
                capture(avstemmingsnøkkel),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            OppdragDto(
                avstemmingsnøkkel.captured,
                behov.fnr,
                FAGSYSTEMID,
                LocalDateTime.now(),
                Oppdragstatus.MOTTATT,
                BELØP,
                null
            )
        }
        every { dao.oppdaterOppdrag(any(), behov.fagsystemId, Oppdragstatus.OVERFØRT) } returns true
        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId) } returns false
        rapid.sendTestMessage(behov.json())
        assertEquals(1, inspektør.size)
        assertEquals(1, connection.inspektør.antall())
        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertOverført(0)
    }

    @Test
    fun `løser utbetalingsbehov med engangsutbetaling`() {
        val avstemmingsnøkkel = CapturingSlot<Long>()

        val behov = utbetalingsbehov.linjer(
            utbetalingslinje
                .grad(null)
                .satstype("ENG")
        )

        every {
            dao.nyttOppdrag(
                any(),
                capture(avstemmingsnøkkel),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            OppdragDto(
                avstemmingsnøkkel.captured,
                behov.fnr,
                behov.fagsystemId,
                LocalDateTime.now(),
                Oppdragstatus.MOTTATT,
                behov.linjer[0].sats,
                null
            )
        }
        every { dao.finnesFraFør(utbetalingsbehov.fnr, utbetalingsbehov.utbetalingId) } returns false
        every { dao.oppdaterOppdrag(any(), FAGSYSTEMID, Oppdragstatus.OVERFØRT) } returns true

        rapid.sendTestMessage(behov.json())
        assertEquals(1, inspektør.size)
        assertEquals(1, connection.inspektør.antall())
        val body = connection.inspektør.melding(0).getBody(String::class.java)
        val unmarshalled = OppdragXml.unmarshal(body, false)
        assertEquals(0, unmarshalled.oppdrag110.oppdragsLinje150[0].grad170.size)
        assertEquals("ENG", unmarshalled.oppdrag110.oppdragsLinje150[0].typeSats)

        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertOverført(0)
    }

    @Test
    fun `løser duplikat utbetalingsbehov`() {
        val behov = utbetalingsbehov
        val avstemmingsnøkler = mutableListOf<Long>()
        fun oppdragDto(status: Oppdragstatus = Oppdragstatus.OVERFØRT) = OppdragDto(
            avstemmingsnøkler.first(),
            behov.fnr,
            FAGSYSTEMID,
            LocalDateTime.now(),
            status,
            BELØP,
            null
        )
        every {
            dao.nyttOppdrag(
                any(),
                capture(avstemmingsnøkler),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } coAnswers { oppdragDto(Oppdragstatus.MOTTATT) }
        every {
            dao.oppdaterOppdrag(
                match { it == avstemmingsnøkler.first() },
                any(),
                Oppdragstatus.OVERFØRT
            )
        } returns true

        every { dao.hentOppdragForSjekksum(any()) } answers { oppdragDto() }
        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId) } returns false
        rapid.sendTestMessage(behov.json())

        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId) } returns true
        every { dao.hentOppdrag(behov.fnr, behov.utbetalingId) } answers { oppdragDto() }
        rapid.sendTestMessage(behov.json())

        assertEquals(2, inspektør.size)
        assertEquals(1, connection.inspektør.antall())
        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertOverført(0)
        assertOverført(1)
    }

    @Test
    fun `Tillater duplikat melding ved AVVIST`() {
        val avstemmingsnøkler = mutableListOf<Long>()
        val behov = utbetalingsbehov

        fun oppdragDto(status: Oppdragstatus = Oppdragstatus.OVERFØRT) = OppdragDto(
            avstemmingsnøkler.first(),
            behov.fnr,
            FAGSYSTEMID,
            LocalDateTime.now(),
            status,
            BELØP,
            null
        )
        every {
            dao.nyttOppdrag(
                any(),
                capture(avstemmingsnøkler),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } coAnswers { oppdragDto(Oppdragstatus.MOTTATT) }
        every {
            dao.oppdaterOppdrag(
                match { it == avstemmingsnøkler.first() },
                any(),
                Oppdragstatus.OVERFØRT
            )
        } returns true

        every { dao.hentOppdragForSjekksum(any()) } answers { oppdragDto(Oppdragstatus.AVVIST) }
        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId) } returns false
        rapid.sendTestMessage(behov.json())

        every { dao.finnesFraFør(behov.fnr, behov.utbetalingId) } returns true
        every { dao.hentOppdrag(behov.fnr, behov.utbetalingId) } answers { oppdragDto(Oppdragstatus.AVVIST) }
        rapid.sendTestMessage(behov.json())

        assertEquals(2, inspektør.size)
        assertEquals(2, connection.inspektør.antall())
        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(0).jmsReplyTo.toString())
        assertEquals("queue:///$REPLY_TO_QUEUE", connection.inspektør.melding(1).jmsReplyTo.toString())
        assertOverført(indeks = 0, antallOverforinger = 2)
        assertOverført(indeks = 1, antallOverforinger = 2)
    }

    @Test
    fun `ignorerer tomme utbetalingslinjer`() {
        val behov = utbetalingsbehov.linjer()
        rapid.sendTestMessage(behov.json())
        assertEquals(0, inspektør.size)
        assertEquals(0, connection.inspektør.antall())
        verify(exactly = 0) {
            dao.nyttOppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `utbetalingsbehov med exception`() {
        every {
            dao.nyttOppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException()
        rapid.sendTestMessage(utbetalingsbehov.json())
        assertEquals(1, inspektør.size)
        assertEquals(0, connection.inspektør.antall())
        assertEquals(BEHOV, inspektør.id(0))
        inspektør.løsning(0, "Utbetaling") {
            assertEquals(Oppdragstatus.FEIL.name, it.path("status").asText())
        }
    }

    private fun assertOverført(indeks: Int, antallOverforinger: Int = 1) {
        assertEquals(BEHOV, inspektør.id(indeks))
        inspektør.løsning(indeks, "Utbetaling") {
            assertEquals(Oppdragstatus.OVERFØRT.name, it.path("status").asText())
            assertDoesNotThrow { it.path("avstemmingsnøkkel").asText().toLong() }
            assertDoesNotThrow { LocalDateTime.parse(it.path("overføringstidspunkt").asText()) }
        }
        val avstemmingsnøkkel = inspektør.løsning(indeks, "Utbetaling")
            .path("avstemmingsnøkkel")
            .asLong()
        verify(exactly = 1) {
            dao.nyttOppdrag(
                fagområde = "SPREF",
                avstemmingsnøkkel = avstemmingsnøkkel,
                utbetalingId = any(),
                sjekksum = any(),
                fødselsnummer = utbetalingsbehov.fnr,
                organisasjonsnummer = ORGNR,
                mottaker = ORGNR,
                tidspunkt = any(),
                fagsystemId = FAGSYSTEMID,
                status = Oppdragstatus.MOTTATT,
                totalbeløp = BELØP,
                originalJson = any()
            )
        }
        verify(exactly = antallOverforinger) {
            dao.oppdaterOppdrag(
                avstemmingsnøkkel,
                FAGSYSTEMID,
                Oppdragstatus.OVERFØRT
            )
        }
    }
}
