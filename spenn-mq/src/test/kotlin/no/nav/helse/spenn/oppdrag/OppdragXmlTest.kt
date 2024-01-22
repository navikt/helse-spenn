package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdragXmlTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val SAKSBEHANDLER = "Navn Navnesen"

        private const val AKSEPTERT_UTEN_FEIL = "00"
        private const val AKSEPTERT_MED_FEIL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"

        private val xmlMapper = XmlMapper()
            .registerModule(JavaTimeModule())
    }

    @Test
    fun marshal() {
        val datoOppdragGjelderFom = LocalDate.EPOCH
        val oppdrag = OppdragDto(
            oppdrag110 = Oppdrag110Dto(
                kodeFagomraade = "SPREF",
                kodeEndring = EndringskodeDto.ENDR,
                fagsystemId = "AF6K73WJKNGY5EK6ZSXFWFU6QA",
                oppdragGjelderId = "12345678911",
                saksbehId = "SPLEIS",
                kodeAksjon = "??",
                utbetFrekvens = "MND",
                datoOppdragGjelderFom = datoOppdragGjelderFom,
                avstemming115 = Avstemming115Dto(
                    nokkelAvstemming = 1L,
                    tidspktMelding = LocalDateTime.now(),
                    kodeKomponent = "??"
                ),
                oppdragsEnhet120 = listOf(
                    OppdragsEnhet120Dto("BOS", "SYK", LocalDate.EPOCH)
                ),
                oppdragsLinje150 = listOf(
                    OppdragsLinje150Dto(
                        delytelseId = 1,
                        refDelytelseId = null,
                        refFagsystemId = null,
                        kodeEndringLinje = EndringskodeDto.NY,
                        kodeKlassifik = "SYKPENG",
                        datoVedtakFom = LocalDate.EPOCH,
                        datoVedtakTom = LocalDate.now(),
                        kodeStatusLinje = StatuskodeLinjeDto.REAK,
                        datoStatusFom = LocalDate.EPOCH,
                        sats = 500,
                        henvisning = "SPEIL",
                        fradragTillegg = FradragTilleggDto.T,
                        typeSats = SatstypeDto.DAG,
                        saksbehId = "SPENN",
                        brukKjoreplan = "N",
                        grad170 = listOf(
                            Grad170Dto("UFOR", 100)
                        ),
                        attestant180 = listOf(
                            Attestant180Dto("SPEIL")
                        )
                    ).apply {
                        utbetalesTilId = "12345678911"
                    }
                )
            )
        )
        val result = OppdragXml.marshal(oppdrag)
        val node = xmlMapper.readTree(result)
        assertTrue(result.contains("<Oppdrag>")) {
            "Oppdrag-XML kan ikke inneholde namespace-deklarasjon, f.eks. <Oppdrag xmlns=\"http://www.trygdeetaten.no/skjema/oppdrag\"> fordi da faller OS sammen"
        }
        assertEquals("1970-01-01", node.path("oppdrag-110").path("datoOppdragGjelderFom").asText())

        val feltnavn = node.path("oppdrag-110").path("oppdrags-linje-150")
            .fieldNames()
            .asSequence()
            .toList()
        val posisjonUtbetalesTilId = feltnavn.indexOf("utbetalesTilId")
        val posisjonGrad = feltnavn.indexOf("grad-170")
        val posisjonAttestant = feltnavn.indexOf("attestant-180")

        assertTrue(posisjonUtbetalesTilId < posisjonGrad) { "OS er sensitiv på plasseringen til utbetalesTilId-feltet" }
        assertTrue(posisjonUtbetalesTilId < posisjonAttestant) { "OS er sensitiv på plasseringen til utbetalesTilId-feltet" }
    }

    @Test
    fun unmarshal() {
        assertNotNull(OppdragXml.unmarshal(kvittering(AKSEPTERT_UTEN_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AKSEPTERT_MED_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AVVIST_FUNKSJONELLE_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AVVIST_TEKNISK_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AVVIST_TEKNISK_FEIL)))
        assertNotNull(OppdragXml.unmarshal(feilmelding()))
        assertNotNull(OppdragXml.unmarshal(rarKvittering(AKSEPTERT_UTEN_FEIL)))
    }

    @Language("XML")
    private fun feilmelding() =
        """<?xml version="1.0" encoding="utf-8"?>
<oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>231-OPPD</systemId>
        <kodeMelding>B606002F</kodeMelding>
        <alvorlighetsgrad>08</alvorlighetsgrad>
        <beskrMelding>Det oppsto en feil ved parsing</beskrMelding>
        <programId>K231B606</programId>
        <sectionNavn>CA00-BEHANDLE-FELT</sectionNavn>
    </mmel>
    <oppdrag-110></oppdrag-110>""" // <-- Ja: OS svarer med ugyldig XML
    private fun kvittering(alvorlighetsgrad: String) =
        kvittering(
            "<ns2:oppdrag xmlns:ns2=\"http://www.trygdeetaten.no/skjema/oppdrag\">",
            "</ns2:oppdrag>",
            alvorlighetsgrad
        )

    private fun rarKvittering(alvorlighetsgrad: String) =
        kvittering("<oppdrag xmlns=\"http://www.trygdeetaten.no/skjema/oppdrag\">", "</Oppdrag>", alvorlighetsgrad)

    private fun kvittering(head: String, foot: String, alvorlighetsgrad: String) =
        """<?xml version="1.0" encoding="utf-8"?>
$head
    <mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>$alvorlighetsgrad</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SPREF</kodeFagomraade>
        <fagsystemId>$UTBETALINGSREF</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>$PERSON</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01</datoOppdragGjelderFom>
        <saksbehId>$SAKSBEHANDLER</saksbehId>
        <avstemming-115>
            <kodeKomponent>SP</kodeKomponent>
            <nokkelAvstemming>$AVSTEMMINGSNØKKEL</nokkelAvstemming>
            <tidspktMelding>2019-09-20T13:31:28.572227</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>1970-01-01</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>1</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-01-01</datoVedtakFom>
            <datoVedtakTom>2019-01-12</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-01-01</datoFom>
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
            <datoVedtakFom>2019-02-13</datoVedtakFom>
            <datoVedtakTom>2019-02-20</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-02-13</datoFom>
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
            <datoVedtakFom>2019-03-18</datoVedtakFom>
            <datoVedtakTom>2019-04-12</datoVedtakTom>
            <sats>1000</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-03-18</datoFom>
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
$foot"""
}
