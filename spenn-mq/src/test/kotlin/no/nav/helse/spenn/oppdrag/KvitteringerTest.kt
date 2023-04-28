package no.nav.helse.spenn.oppdrag

import io.mockk.clearAllMocks
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.TestConnection
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.AKSEPTERT_MED_FEIL
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.AKSEPTERT_UTEN_FEIL
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.AVVIST_FUNKSJONELLE_FEIL
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.AVVIST_TEKNISK_FEIL
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.UGYLDIG_FEILKODE
import no.nav.helse.spenn.oppdrag.Kvittering.Companion.kvittering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class KvitteringerTest {
    private companion object {
        const val MOTTAK_QUEUE = "statusQueue"
    }

    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Kvitteringer(this, Jms( connection, "sendQueue", MOTTAK_QUEUE))
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        connection.reset()
        rapid.reset()
    }

    @Test
    fun `akseptert uten feil`() {
        håndter(Oppdragstatus.AKSEPTERT, AKSEPTERT_UTEN_FEIL)
    }

    @Test
    fun `akseptert med feil`() {
        håndter(Oppdragstatus.AKSEPTERT_MED_FEIL, AKSEPTERT_MED_FEIL)
    }

    @Test
    fun `avvist med funksjonelle feil`() {
        håndter(Oppdragstatus.AVVIST, AVVIST_FUNKSJONELLE_FEIL)
    }

    @Test
    fun `avvist med teknisk feil`() {
        håndter(Oppdragstatus.AVVIST, AVVIST_TEKNISK_FEIL)
    }

    @Test
    fun `feil i forståelse av melding`() {
        håndter(Oppdragstatus.FEIL, UGYLDIG_FEILKODE)
    }

    private fun håndter(status: Oppdragstatus, alvorlighetsgrad: String) {
        val kvittering = Kvittering(alvorlighetsgrad=alvorlighetsgrad).toXml()
        connection.sendMessage(kvittering)
        assertKvittering(status, alvorlighetsgrad, kvittering)
    }

    private fun assertKvittering(status: Oppdragstatus, alvorlighetsgrad: String, melding: String) {
        assertEquals(2, rapid.inspektør.size)
        assertEquals("oppdrag_kvittering", rapid.inspektør.field(0, "@event_name").asText())
        assertEquals("transaksjon_status", rapid.inspektør.field(1, "@event_name").asText())
        assertEquals(kvittering.fødselsnummer, rapid.inspektør.field(1, "fødselsnummer").asText())
        assertEquals(kvittering.avstemmingsnøkkel, rapid.inspektør.field(1, "avstemmingsnøkkel").asLong())
        assertEquals(kvittering.fagsystemId, rapid.inspektør.field(1, "fagsystemId").asText())
        assertEquals(status.name, rapid.inspektør.field(1, "status").asText())
        assertEquals(alvorlighetsgrad, rapid.inspektør.field(1, "feilkode_oppdrag").asText())
        assertTrue(rapid.inspektør.field(1, "beskrivelse").asText().isNotBlank())
        assertEquals(melding, rapid.inspektør.field(1, "originalXml").asText())
        assertDoesNotThrow { UUID.fromString(rapid.inspektør.field(1, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(rapid.inspektør.field(1, "@opprettet").asText()) }
    }
}
