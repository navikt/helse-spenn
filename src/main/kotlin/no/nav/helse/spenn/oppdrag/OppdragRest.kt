package no.nav.helse.spenn.oppdrag

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import no.nav.helse.spenn.simulering.OppdragMapperForSimulering
import no.nav.helse.spenn.simulering.SimuleringService
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/oppdrag")
class OppdragRest(val mqSender: OppdragMQSender, val jaxb : JAXBOppdrag, val simuleringService : SimuleringService,
                  val simuleringsmapper : OppdragMapperForSimulering) {

    @PostMapping
    fun sendOppdrag(@RequestBody oppdragXml: String) {
        mqSender.sendOppdrag(jaxb.toOppdrag(oppdragXml))
    }


    @PostMapping("/simulering")
    fun sendSimulering() {
        val enOppdragsLinje = OppdragsLinje(id = "1234567890", datoFom = LocalDate.now().minusWeeks(2),
                datoTom = LocalDate.now(), sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.MÅNEDLIG,
                utbetalesTil = "995816598")
        val utbetalingsOppdrag = UtbetalingsOppdrag(id = "20190408084501", operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "995816598", oppdragslinje = listOf(enOppdragsLinje))
        simuleringService.simulerOppdrag(simuleringsmapper.mapOppdragToSimuleringRequest(utbetalingsOppdrag))
    }

}