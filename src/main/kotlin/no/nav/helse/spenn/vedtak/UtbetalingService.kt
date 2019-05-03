package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.toOppdrag
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.helse.spenn.simulering.SimuleringService

import org.springframework.stereotype.Service

@Service
class UtbetalingService(val simuleringService: SimuleringService,
                        val oppdragSender: OppdragMQSender,
                        val oppdragStateService: OppdragStateService,
                        val aktørTilFnrMapper: AktørTilFnrMapper) {

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        val utbetaling = oppdrag.vedtak.tilUtbetaling(fagSystemId = oppdrag.id!!)
        val result = simuleringService
                .simulerOppdrag(utbetaling.toSimuleringRequest())
        oppdrag.simuleringResult = result
        return oppdragStateService.saveOppdragState(oppdrag)
    }


    fun sendUtbetalingOppdragMQ(oppdrag: OppdragStateDTO) {
        val utbetaling = oppdrag.vedtak.tilUtbetaling(aktørTilFnrMapper, oppdrag.id!!)
        oppdragSender.sendOppdrag(utbetaling.toOppdrag())
    }


}