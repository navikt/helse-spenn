package no.nav.helse.integrasjon.okonomi.oppdrag

import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import no.trygdeetaten.skjema.oppdrag.TkodeStatus
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory
import java.time.ZoneId
import java.util.GregorianCalendar
import java.time.LocalDate



class OppdragMapperTest {

    @Test
    fun mapToXml() {
        val objectFactory = ObjectFactory()
        val nowXMLGregorian = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault())))

        val avstemming115 = objectFactory.createAvstemming115().apply {
            kodeKomponent = KomponentKode.VLSP.kode
            nokkelAvstemming = "123" // ?
            tidspktMelding = "2019-01-01-12.00.01.123"
        }

        val bilagstype = objectFactory.createBilagstype113().apply {
            bilagsType = BilagsTypeKode.ORDINÆR.kode
        }

        val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
            enhet = "8092" //tknr ?
            typeEnhet = "BOS"
            datoEnhetFom = nowXMLGregorian
        }

        // beløpsgrense ?
        // tekst ?

        val oppdragslinje = objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje = EndringsKode.NY.kode
            kodeStatusLinje = TkodeStatusLinje.OPPH
            datoStatusFom = nowXMLGregorian
            vedtakId = "123"
            delytelseId = "321"
            kodeKlassifik = "SP-HVILKENKODE" // må inn oppdragssystemets kodeverk
            datoVedtakFom = nowXMLGregorian
            datoVedtakTom = nowXMLGregorian
            sats = BigDecimal(1230)
            fradragTillegg = TfradragTillegg.F
            typeSats = SatsTypeKode.MÅNEDLIG.kode
            skyldnerId = "123456" //behandlende enhetsorgnr
            utbetalesTilId = "333333" //fnr eller orgnr
            saksbehId = "Z123456"
            brukKjoreplan = "B" // ?
        }
        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = AksjonsKode.OPPDATER.kode
            kodeEndring = EndringsKode.NY.kode
            kodeFagomraade = FagOmrådeKode.HELSE.kode
            kodeStatus = TkodeStatus.HVIL
            fagsystemId = "123" // har ikke peiling
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            oppdragGjelderId = "123" //orgnr eller fnr
            saksbehId = "Z123456"
            bilagstype113 = bilagstype
            oppdragsEnhet120.add(oppdragsEnhet)
            this.avstemming115 = avstemming115
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

}