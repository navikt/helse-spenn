package no.nav.helse.spenn.utbetaling

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.TestConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class KvitteringerTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val FAGSYSTEMID = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val MOTTAK_QUEUE = "statusQueue"

        private const val AKSEPTERT_UTEN_FEIL = "00"
        private const val AKSEPTERT_MED_FEIL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"
        private const val UGYLDIG_FEILKODE = "??"
    }

    private val dao = mockk<OppdragDao>()
    private val connection = TestConnection()
    private val rapid = TestRapid().apply {
        Kvitteringer(this, connection, MOTTAK_QUEUE, dao)
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
            Oppdragstatus.AVVIST,
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
        every {
            dao.oppdaterOppdrag(any(), any(), any(), any(), any(), any())
        } returns true

        val kvittering = kvittering(alvorlighetsgrad)
        connection.sendMessage(kvittering)
        assertKvittering(status, alvorlighetsgrad, kvittering)
    }

    private fun assertKvittering(status: Oppdragstatus, alvorlighetsgrad: String, melding: String) {
        assertEquals(1, rapid.inspektør.size)
        assertEquals("transaksjon_status", rapid.inspektør.field(0, "@event_name").asText())
        assertEquals(PERSON, rapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals(AVSTEMMINGSNØKKEL, rapid.inspektør.field(0, "avstemmingsnøkkel").asLong())
        assertEquals(FAGSYSTEMID, rapid.inspektør.field(0, "fagsystemId").asText())
        assertEquals(status.name, rapid.inspektør.field(0, "status").asText())
        assertEquals(alvorlighetsgrad, rapid.inspektør.field(0, "feilkode_oppdrag").asText())
        assertTrue(rapid.inspektør.field(0, "beskrivelse").asText().isNotBlank())
        assertEquals(melding, rapid.inspektør.field(0, "originalXml").asText())
        assertDoesNotThrow { UUID.fromString(rapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(rapid.inspektør.field(0, "@opprettet").asText()) }
        verify(exactly = 1) { dao.oppdaterOppdrag(
            AVSTEMMINGSNØKKEL,
            FAGSYSTEMID, status, any(), alvorlighetsgrad, melding) }
    }

    private fun kvittering(alvorlighetsgrad: String) = """<?xml version="1.0" encoding="utf-8"?>
<ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>$alvorlighetsgrad</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SPREF</kodeFagomraade>
        <fagsystemId>$FAGSYSTEMID</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>$PERSON</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01+01:00</datoOppdragGjelderFom>
        <saksbehId>$SAKSBEHANDLER</saksbehId>
        <avstemming-115>
            <kodeKomponent>SP</kodeKomponent>
            <nokkelAvstemming>$AVSTEMMINGSNØKKEL</nokkelAvstemming>
            <tidspktMelding>2019-09-20-13.31.28.572227</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>1970-01-01+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>1</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-01-01+01:00</datoVedtakFom>
            <datoVedtakTom>2019-01-12+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-01-01+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>50</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>2</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-02-13+01:00</datoVedtakFom>
            <datoVedtakTom>2019-02-20+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-02-13+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>70</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>3</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-03-18+01:00</datoVedtakFom>
            <datoVedtakTom>2019-04-12+02:00</datoVedtakTom>
            <sats>1000</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20+02:00</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-03-18+01:00</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""
}
