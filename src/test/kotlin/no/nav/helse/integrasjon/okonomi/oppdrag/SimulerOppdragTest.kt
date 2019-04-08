package no.nav.helse.integrasjon.okonomi.oppdrag

import no.nav.helse.spenn.oppdrag.OppdragsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.OppdragMapperForSimulering
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigDecimal
import java.time.LocalDate
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class SimulerOppdragTest {

    @Test
    fun mapSimuleringsOppdrag() {
        val mapper = OppdragMapperForSimulering()
        val enOppdragsLinje = OppdragsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "995816598")
        val utbetalingsOppdrag = UtbetalingsOppdrag(id = "20190408084501", operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "995816598", oppdragslinje = listOf(enOppdragsLinje))
        val simuleringRequest = mapper.mapOppdragToSimuleringRequest(utbetalingsOppdrag)
        val jaxbContext = JAXBContext.newInstance(SimulerBeregningRequest::class.java)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val stringWriter = StringWriter()
        marshaller.marshal(simuleringRequest, stringWriter)
        println(stringWriter)
        Assertions.assertEquals("00995816598", simuleringRequest.request.oppdrag.oppdragGjelderId)
    }
}