package no.nav.helse.integrasjon.okonomi.oppdrag

import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import no.trygdeetaten.skjema.oppdrag.TkodeStatus
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory
import java.time.ZoneId
import java.util.GregorianCalendar
import java.time.LocalDate
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource


class OppdragMapperTest {

    @Test
    fun createOppdragXml() {
        // minumum vedtak må innholde 110,120,150,180
        val objectFactory = ObjectFactory()
        val nowXMLGregorian = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault())))
        val weeksfromNow = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(LocalDate.now().plusDays(14).atStartOfDay(ZoneId.systemDefault())))
        val orgnr_fnr = "9999_FNR_ELLER_ORGNR"
        val komponentKode = "SP_VET_IKKE"
        val appNavn = "helse-spenn"
        val SYKEPENGEENHET="4151"

        val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
            enhet = SYKEPENGEENHET
            typeEnhet = "BOS"
            datoEnhetFom = nowXMLGregorian
        }

        // beløpsgrense ?
        // tekst ?
        val attestant = objectFactory.createAttestant180().apply {
            attestantId = appNavn
            datoUgyldigFom= weeksfromNow
        }

        val oppdragslinje = objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje = EndringsKode.NY.kode
            vedtakId = "123"
            delytelseId = "321"
            kodeKlassifik = komponentKode // må inn oppdragssystemets kodeverk
            datoVedtakFom = nowXMLGregorian
            datoVedtakTom = nowXMLGregorian
            sats = BigDecimal(1230)
            fradragTillegg = TfradragTillegg.T
            typeSats = SatsTypeKode.ENGANGSBELØP.kode
            saksbehId = appNavn
            brukKjoreplan = "N"
            attestant180.add(attestant)

        }
        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = AksjonsKode.OPPDATER.kode
            kodeEndring = EndringsKode.NY.kode
            kodeFagomraade = komponentKode
            kodeStatus = TkodeStatus.NY
            fagsystemId = "123" // har ikke peiling
            utbetFrekvens = UtbetalingsfrekvensKode.ENGANGSUTBETALING.kode
            oppdragGjelderId = orgnr_fnr //orgnr eller fnr
            saksbehId = appNavn
            oppdragsEnhet120.add(oppdragsEnhet)
            oppdragsLinje150.add(oppdragslinje)
        }

        val oppdrag = objectFactory.createOppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(oppdrag, stringWriter)
        println(stringWriter)
    }

    @Test
    fun readOppdragKvitteringXml() {
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val unmarshaller = jaxbContext.createUnmarshaller()
        val xmlInputFactory = XMLInputFactory.newFactory().apply {
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
        }
        val source = StreamSource(StringReader(kvittering))
        val unmarshal = unmarshaller.unmarshal(xmlInputFactory.createXMLStreamReader(source),
                OppdragSkjemaConstants.JAXB_CLASS)
        val kvitteringOppdrag = unmarshal.value

        println(kvitteringOppdrag.mmel.alvorlighetsgrad)
    }

}

val kvittering = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>systemId</systemId>
        <alvorlighetsgrad>00</alvorlighetsgrad>
        <beskrMelding>Oppdrag behandlet</beskrMelding>
        <sqlKode>sqlKode</sqlKode>
        <sqlState>sqlState</sqlState>
        <sqlMelding>sqlMelding</sqlMelding>
        <mqCompletionKode>mqCompletionKode</mqCompletionKode>
        <mqReasonKode>mqReasonKode</mqReasonKode>
        <programId>programId</programId>
        <sectionNavn>sectionNavn</sectionNavn>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeStatus>NY</kodeStatus>
        <kodeFagomraade>SP_VET_IKKE</kodeFagomraade>
        <fagsystemId>123</fagsystemId>
        <utbetFrekvens>ENG</utbetFrekvens>
        <oppdragGjelderId>9999_FNR_ELLER_ORGNR</oppdragGjelderId>
        <saksbehId>helse-spenn</saksbehId>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>2019-03-26+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <vedtakId>123</vedtakId>
            <delytelseId>321</delytelseId>
            <kodeKlassifik>SP_VET_IKKE</kodeKlassifik>
            <datoVedtakFom>2019-03-26+01:00</datoVedtakFom>
            <datoVedtakTom>2019-03-26+01:00</datoVedtakTom>
            <sats>1230</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>ENG</typeSats>
            <skyldnerId>123456</skyldnerId>
            <brukKjoreplan>B</brukKjoreplan>
            <saksbehId>helse-spenn</saksbehId>
            <utbetalesTilId>9999_FNR_ELLER_ORGNR</utbetalesTilId>
            <attestant-180>
                <attestantId>helse-spenn</attestantId>
                <datoUgyldigFom>2019-04-09+02:00</datoUgyldigFom>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""

val oppdrag = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeStatus>NY</kodeStatus>
        <kodeFagomraade>SP_VET_IKKE</kodeFagomraade>
        <fagsystemId>123</fagsystemId>
        <utbetFrekvens>ENG</utbetFrekvens>
        <oppdragGjelderId>9999_FNR_ELLER_ORGNR</oppdragGjelderId>
        <saksbehId>helse-spenn</saksbehId>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>2019-03-26+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <vedtakId>123</vedtakId>
            <delytelseId>321</delytelseId>
            <kodeKlassifik>SP_VET_IKKE</kodeKlassifik>
            <datoVedtakFom>2019-03-26+01:00</datoVedtakFom>
            <datoVedtakTom>2019-03-26+01:00</datoVedtakTom>
            <sats>1230</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>ENG</typeSats>
            <skyldnerId>123456</skyldnerId>
            <brukKjoreplan>B</brukKjoreplan>
            <saksbehId>helse-spenn</saksbehId>
            <utbetalesTilId>9999_FNR_ELLER_ORGNR</utbetalesTilId>
            <attestant-180>
                <attestantId>helse-spenn</attestantId>
                <datoUgyldigFom>2019-04-09+02:00</datoUgyldigFom>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""