package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.springframework.stereotype.Component

@Component
class SimuleringClient(val simuleringService: SimuleringService, val oppdragMapper : OppdragMapperForSimulering) {

    fun simulerUtbetalingsOppdrag(utbetaling : UtbetalingsOppdrag) {
        simuleringService.simulerOppdrag(oppdragMapper.mapOppdragToSimuleringRequest(utbetaling))
    }


}