package no.nav.helse.spenn.vedtak

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.metrics.SIMULERING
import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.toOppdrag
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service

@Service
class UtbetalingService(val simuleringService: SimuleringService,
                        val oppdragSender: OppdragMQSender,
                        val oppdragStateService: OppdragStateService,
                        val aktørTilFnrMapper: AktørTilFnrMapper,
                        val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(UtbetalingService::class.java)

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        log.info("simulering for ${oppdrag.soknadId} fagsystemId ${oppdrag.id}")
        val result = simuleringService
                .simulerOppdrag(oppdrag.utbetalingsOppdrag.toSimuleringRequest(oppdrag.id.toString()))
        oppdrag.status = when(result.status) {
            Status.OK -> OppdragStateStatus.SIMULERING_OK
            else -> OppdragStateStatus.FEIL
        }
        oppdrag.simuleringResult = result
        meterRegistry.counter(SIMULERING, "status", oppdrag.status.name).increment()
        return oppdragStateService.saveOppdragState(oppdrag)
    }


    fun sendUtbetalingOppdragMQ(oppdrag: OppdragStateDTO) {
        log.info("sender til Oppdragsystemet for ${oppdrag.soknadId} fagsystemId ${oppdrag.id}")
        oppdragSender.sendOppdrag(oppdrag.utbetalingsOppdrag.toOppdrag(oppdrag.id.toString()))

    }


}