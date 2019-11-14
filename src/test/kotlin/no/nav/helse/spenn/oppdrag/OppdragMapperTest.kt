package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.etEnkeltBehov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertNull


class OppdragMapperTest {

    @Test
    fun createOppdragXml() {
        val maksDato = LocalDate.now().plusYears(1).minusDays(50)
        val vedtakFom = LocalDate.now().minusWeeks(2)
        val vedtakTom = LocalDate.now()
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = vedtakFom,
                datoTom = vedtakTom, sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "12121212345", utbetalingsLinje = listOf(enOppdragsLinje),
                behov = etEnkeltBehov(maksdato = maksDato))
        val oppdragState = OppdragStateDTO(
            id = 1L,
            sakskompleksId = UUID.randomUUID(),
            utbetalingsreferanse = "1001",
            utbetalingsOppdrag = utbetaling
        )
        val oppdrag = oppdragState.toOppdrag()
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(oppdrag, stringWriter)
        println(stringWriter)
        assertEquals(AksjonsKode.OPPDATER.kode, oppdrag.oppdrag110.kodeAksjon)
        assertEquals("12121212345", oppdrag.oppdrag110.oppdragGjelderId)
        assertNull(oppdrag.oppdrag110.oppdragsLinje150[0].utbetalesTilId)
        assertEquals("00995816598", oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.refunderesId)
        assertEquals(DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(
                maksDato.atStartOfDay(ZoneId.systemDefault()))),
                oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.maksDato)
        assertEquals(DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(
                vedtakFom.atStartOfDay(ZoneId.systemDefault()))),
                oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.datoFom)
    }


}


