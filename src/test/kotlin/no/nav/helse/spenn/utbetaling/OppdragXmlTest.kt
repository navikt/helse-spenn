package no.nav.helse.spenn.utbetaling

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

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
        private const val UGYLDIG_FEILKODE = "??"
    }

    @Test
    fun unmarshal() {
        assertNotNull(OppdragXml.unmarshal(kvittering(AKSEPTERT_UTEN_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AKSEPTERT_MED_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AVVIST_FUNKSJONELLE_FEIL)))
        assertNotNull(OppdragXml.unmarshal(kvittering(AVVIST_TEKNISK_FEIL)))
        assertNotNull(OppdragXml.unmarshal(rarKvittering(AKSEPTERT_UTEN_FEIL)))
    }

    private fun kvittering(alvorlighetsgrad: String) =
        kvittering("<ns2:oppdrag xmlns:ns2=\"http://www.trygdeetaten.no/skjema/oppdrag\">", "</ns2:oppdrag>", alvorlighetsgrad)

    private fun rarKvittering(alvorlighetsgrad: String) =
        kvittering("<oppdrag xmlns=\"http://www.trygdeetaten.no/skjema/oppdrag\">", "</Oppdrag>", alvorlighetsgrad)

    private fun kvittering(head: String, foot: String, alvorlighetsgrad: String) = """<?xml version="1.0" encoding="utf-8"?>
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
$foot"""
}
