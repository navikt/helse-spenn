package no.nav.helse.integrasjon.okonomi.oppdrag

import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class SimulerOppdragTest {

    @Test
    fun mapSimuleringsOppdrag() {
        val enOppdragsLinje = UtbetalingsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "123456789", grad = BigInteger.valueOf(100))
        val utbetalingsOppdrag = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "123456789", utbetalingsLinje = listOf(enOppdragsLinje))
        val simuleringRequest = utbetalingsOppdrag.toSimuleringRequest("2019040808450")
        val jaxbContext = JAXBContext.newInstance(SimulerBeregningRequest::class.java)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(simuleringRequest, stringWriter)
        println(stringWriter)
        Assertions.assertEquals("00123456789", simuleringRequest.request.oppdrag.oppdragGjelderId)
    }
}