package no.nav.helse.spenn.utbetaling

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.rapids_rivers.toUUID
import no.nav.helse.spenn.Jms
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.e2e.Kvittering
import no.nav.helse.spenn.e2e.Kvittering.Companion.AKSEPTERT_MED_FEIL
import no.nav.helse.spenn.e2e.Kvittering.Companion.AKSEPTERT_UTEN_FEIL
import no.nav.helse.spenn.e2e.Kvittering.Companion.AVVIST_FUNKSJONELLE_FEIL
import no.nav.helse.spenn.e2e.Kvittering.Companion.AVVIST_TEKNISK_FEIL
import no.nav.helse.spenn.e2e.Kvittering.Companion.UGYLDIG_FEILKODE
import no.nav.helse.spenn.e2e.Kvittering.Companion.kvittering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class KvitteringerTest {
    companion object {
        const val MOTTAK_QUEUE = "statusQueue"


    }

    private val dao = mockk<OppdragDao>()
    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Kvitteringer(this, Jms( connection, "sendQueue", MOTTAK_QUEUE), dao)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `akseptert uten feil`() {
        håndter(
            Oppdragstatus.AKSEPTERT,
            AKSEPTERT_UTEN_FEIL
        )
    }

    @Test
    fun `akseptert med feil`() {
        håndter(
            Oppdragstatus.AKSEPTERT_MED_FEIL,
            AKSEPTERT_MED_FEIL
        )
    }

    @Test
    fun `avvist med funksjonelle feil`() {
        håndter(
            Oppdragstatus.AVVIST,
            AVVIST_FUNKSJONELLE_FEIL
        )
    }

    @Test
    fun `avvist med teknisk feil`() {
        håndter(
            Oppdragstatus.FEIL,
            AVVIST_TEKNISK_FEIL
        )
    }

    @Test
    fun `feil i forståelse av melding`() {
        håndter(
            Oppdragstatus.FEIL,
            UGYLDIG_FEILKODE
        )
    }

    private fun håndter(status: Oppdragstatus, alvorlighetsgrad: String) {
        val behov = JsonMessage.newNeed(listOf("Utbetaling"))
        every {
            dao.oppdaterOppdrag(any(), any(), any(), any(), any(), any())
        } returns true
        every {
            dao.hentBehovForOppdrag(any())
        } returns behov

        val kvittering = Kvittering(alvorlighetsgrad=alvorlighetsgrad).toXml()
        connection.sendMessage(kvittering)
        assertKvittering(status, alvorlighetsgrad, kvittering, behov.id.toUUID())
    }

    private fun assertKvittering(status: Oppdragstatus, alvorlighetsgrad: String, melding: String, behovId: UUID) {
        assertEquals(2, rapid.inspektør.size)
        assertEquals("oppdrag_kvittering", rapid.inspektør.field(0, "@event_name").asText())
        assertEquals("transaksjon_status", rapid.inspektør.field(1, "@event_name").asText())
        assertEquals(behovId.toString(), rapid.inspektør.message(1).path("@forårsaket_av").path("id").asText())
        assertEquals(kvittering.fødselsnummer, rapid.inspektør.field(1, "fødselsnummer").asText())
        assertEquals(kvittering.avstemmingsnøkkel, rapid.inspektør.field(1, "avstemmingsnøkkel").asLong())
        assertEquals(kvittering.fagsystemId, rapid.inspektør.field(1, "fagsystemId").asText())
        assertEquals(status.name, rapid.inspektør.field(1, "status").asText())
        assertEquals(alvorlighetsgrad, rapid.inspektør.field(1, "feilkode_oppdrag").asText())
        assertTrue(rapid.inspektør.field(1, "beskrivelse").asText().isNotBlank())
        assertEquals(melding, rapid.inspektør.field(1, "originalXml").asText())
        assertDoesNotThrow { UUID.fromString(rapid.inspektør.field(1, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(rapid.inspektør.field(1, "@opprettet").asText()) }
        verify(exactly = 1) {
            dao.oppdaterOppdrag(
                kvittering.avstemmingsnøkkel,
                kvittering.fagsystemId, status, any(), alvorlighetsgrad, melding
            )
        }
    }
}
