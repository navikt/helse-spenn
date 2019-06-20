package no.nav.helse.spenn.oppdrag

import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class SimulerOppdragTest {

    @Test
    fun mapSimuleringsOppdrag() {
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "123456789", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "123456789", utbetalingsLinje = listOf(enOppdragsLinje))
        val oppdragState = OppdragStateDTO(id = 1L, soknadId = UUID.randomUUID(),
                utbetalingsOppdrag = utbetaling)
        val simuleringRequest = oppdragState.toSimuleringRequest()
        val jaxbContext = JAXBContext.newInstance(SimulerBeregningRequest::class.java)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(simuleringRequest, stringWriter)
        println(stringWriter)
        Assertions.assertEquals("00123456789", simuleringRequest.request.oppdrag.oppdragGjelderId)
    }
}