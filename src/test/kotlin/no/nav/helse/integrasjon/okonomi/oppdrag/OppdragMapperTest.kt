package no.nav.helse.integrasjon.okonomi.oppdrag

import no.nav.helse.spenn.oppdrag.OppdragMapper
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource


class OppdragMapperTest {

    val oppdragMapper = OppdragMapper()

    @Test
    fun createOppdragXml() {
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(id = "20190401110001", operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "995816598", utbetalingsLinje = listOf(enOppdragsLinje))

        val oppdrag = oppdragMapper.mapUtbetalingsOppdrag(utbetaling)
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(oppdrag, stringWriter)
        println(stringWriter)
        assertEquals(AksjonsKode.OPPDATER.kode, oppdrag.oppdrag110.kodeAksjon)
        assertEquals("00995816598", oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals("00995816598", oppdrag.oppdrag110.oppdragsLinje150[0].utbetalesTilId)
    }

    @Test
    fun readOppdragKvitteringXml() {
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val unmarshaller = jaxbContext.createUnmarshaller()
        val xmlInputFactory = XMLInputFactory.newInstance().apply {
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
        <kodeMelding>00</kodeMelding>
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
