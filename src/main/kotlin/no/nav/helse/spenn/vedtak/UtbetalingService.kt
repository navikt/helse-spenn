package no.nav.helse.spenn.vedtak

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.OppdragMapper
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.OppdragMapperForSimulering
import no.nav.helse.spenn.simulering.SimuleringService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UtbetalingService(@Autowired val simuleringService: SimuleringService,
                        @Autowired val oppdragMapperForSimulering: OppdragMapperForSimulering,
                        @Autowired val oppdragSender: OppdragMQSender,
                        @Autowired val oppdragMapper: OppdragMapper) {

    fun sendUtbetalingToOS(utbetaling: UtbetalingsOppdrag) {
        if (utbetaling.operasjon == AksjonsKode.SIMULERING) {
            simuleringService.simulerOppdrag(oppdragMapperForSimulering.mapOppdragToSimuleringRequest(utbetaling))
        }
        else {
            oppdragSender.sendOppdrag(oppdragMapper.mapUtbetalingsOppdrag(utbetaling))
        }

    }
}