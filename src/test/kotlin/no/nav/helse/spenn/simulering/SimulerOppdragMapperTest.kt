package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import kotlin.test.assertNull

class SimulerOppdragMapperTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun mapSimuleringsOppdrag() {
        val maksDato = LocalDate.now().plusYears(1).minusDays(50)
        val vedtakFom = LocalDate.now().minusWeeks(2)
        val vedtakTom = LocalDate.now()
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = vedtakFom,
                datoTom = vedtakTom, sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "123456789", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "12121212345", utbetalingsLinje = listOf(enOppdragsLinje),
                vedtak = Vedtak(
                        soknadId = UUID.randomUUID(),
                        maksDato = maksDato,
                        aktorId = "12341234",
                        vedtaksperioder = emptyList()
                ))
        val oppdragState = OppdragStateDTO(id = 1L, soknadId = UUID.randomUUID(),
                utbetalingsOppdrag = utbetaling)
        val simuleringRequest = oppdragState.toSimuleringRequest()
        val jaxbContext = JAXBContext.newInstance(SimulerBeregningRequest::class.java)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(simuleringRequest, stringWriter)
        println(stringWriter)

        Assertions.assertEquals("12121212345", simuleringRequest.request.oppdrag.oppdragGjelderId)
        assertNull(simuleringRequest.request.oppdrag.oppdragslinje.first().utbetalesTilId)
        Assertions.assertEquals("00123456789", simuleringRequest.request.oppdrag.oppdragslinje[0].refusjonsInfo.refunderesId)
        Assertions.assertEquals(maksDato.format(formatter), simuleringRequest.request.oppdrag.oppdragslinje[0].refusjonsInfo.maksDato)
        Assertions.assertEquals(vedtakFom.format(formatter), simuleringRequest.request.oppdrag.oppdragslinje[0].refusjonsInfo.datoFom)

    }
}