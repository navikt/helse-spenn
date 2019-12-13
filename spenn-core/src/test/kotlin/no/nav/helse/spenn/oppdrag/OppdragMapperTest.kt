package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.core.avstemmingsnokkelFormatter
import no.nav.helse.spenn.oppdrag.dao.TransaksjonDTO
import no.nav.helse.spenn.testsupport.etEnkeltBehov
import no.nav.helse.spenn.testsupport.etUtbetalingsOppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertNull

class OppdragMapperTest {

    private val maksDato = LocalDate.now().plusYears(1).minusDays(50)
    private val vedtakFom = LocalDate.now().minusWeeks(2)
    private val vedtakTom = LocalDate.now()

    private fun enDTO(): TransaksjonDTO {
        val enOppdragsLinje = UtbetalingsLinje(
            id = "1234567890",
            datoFom = vedtakFom,
            datoTom = vedtakTom,
            sats = BigDecimal.valueOf(1230),
            satsTypeKode = SatsTypeKode.MÅNEDLIG,
            utbetalesTil = "995816598",
            grad = BigInteger.valueOf(100)
        )
        val utbetalingTemplate = etUtbetalingsOppdrag()
        val utbetaling = utbetalingTemplate.copy(
            oppdragGjelder = "12121212345",
            utbetalingsreferanse = "1001",
            utbetaling = utbetalingTemplate.utbetaling!!.copy(
                utbetalingsLinjer = listOf(enOppdragsLinje),
                maksdato = maksDato
            )
        )
        return TransaksjonDTO(
            id = 1L,
            utbetalingsreferanse = utbetaling.utbetalingsreferanse,
            utbetalingsOppdrag = utbetaling,
            nokkel = LocalDateTime.now()
        )
    }

    @Test
    fun `mapping av oppdrag request fungerer`() {
        val oppdragState = enDTO()
        val oppdrag = oppdragState.oppdragRequest
        val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(oppdrag, stringWriter)

        assertEquals(AksjonsKode.OPPDATER.kode, oppdrag.oppdrag110.kodeAksjon)
        assertEquals("12121212345", oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals("1001", oppdrag.oppdrag110.fagsystemId)
        assertEquals(avstemmingsnokkelFormatter.format(oppdragState.nokkel), oppdrag.oppdrag110.avstemming115.nokkelAvstemming)
        assertNull(oppdrag.oppdrag110.oppdragsLinje150[0].utbetalesTilId)
        assertEquals("00995816598", oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.refunderesId)
        assertEquals(
            DatatypeFactory.newInstance().newXMLGregorianCalendar(
                GregorianCalendar.from(
                    maksDato.atStartOfDay(ZoneId.systemDefault())
                )
            ),
            oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.maksDato
        )
        assertEquals(
            DatatypeFactory.newInstance().newXMLGregorianCalendar(
                GregorianCalendar.from(
                    vedtakFom.atStartOfDay(ZoneId.systemDefault())
                )
            ),
            oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.datoFom
        )
    }

    @Test
    fun `mapping skal feile hvis nøkkel ikke er satt`() {
       assertThrows<KotlinNullPointerException> { enDTO().copy(nokkel = null).oppdragRequest }
    }
}


