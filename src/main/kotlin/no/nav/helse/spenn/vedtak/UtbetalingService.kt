package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.toOppdrag
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status

import org.springframework.stereotype.Service

@Service
class UtbetalingService(val simuleringService: SimuleringService,
                        val oppdragSender: OppdragMQSender,
                        val oppdragStateService: OppdragStateService,
                        val aktørTilFnrMapper: AktørTilFnrMapper) {

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        val result = simuleringService
                .simulerOppdrag(oppdrag.utbetalingsOppdrag.toSimuleringRequest(oppdrag.id.toString()))
        oppdrag.status = when(result.status) {
            Status.OK -> OppdragStateStatus.SIMULERING_OK
            else -> OppdragStateStatus.FEIL
        }
        oppdrag.simuleringResult = result
        return oppdragStateService.saveOppdragState(oppdrag)
    }


    fun sendUtbetalingOppdragMQ(oppdrag: OppdragStateDTO) {
        oppdragSender.sendOppdrag(oppdrag.utbetalingsOppdrag.toOppdrag(oppdrag.id.toString()))
    }


}