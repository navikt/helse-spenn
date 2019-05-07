package no.nav.helse.integrasjon.okonomi.oppdrag

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.*
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

    @Test
    fun createOppdragXml() {
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "995816598", utbetalingsLinje = listOf(enOppdragsLinje))

        val oppdrag = utbetaling.toOppdrag("oppdragId")
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


}


