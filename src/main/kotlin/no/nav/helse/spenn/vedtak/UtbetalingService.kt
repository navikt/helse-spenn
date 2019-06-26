package no.nav.helse.spenn.vedtak

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.metrics.SIMULERING
import no.nav.helse.spenn.metrics.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.metrics.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.toOppdrag
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service

@Service
class UtbetalingService(val simuleringService: SimuleringService,
                        val oppdragSender: OppdragMQSender,
                        val oppdragStateService: OppdragStateService,
                        val meterRegistry: MeterRegistry) {
    companion object {
        private val log = LoggerFactory.getLogger(UtbetalingService::class.java)
    }

    fun runSimulering(oppdrag: OppdragStateDTO): OppdragStateDTO {
        log.info("simulering for ${oppdrag.soknadId}")
        val result = callSimulering(oppdrag)

        val status = when (result.status) {
            Status.OK -> OppdragStateStatus.SIMULERING_OK
            else -> OppdragStateStatus.SIMULERING_FEIL
        }
        simuleringMetrics(result)
        val updated = oppdrag.copy(simuleringResult = result, status = status)
        return oppdragStateService.saveOppdragState(updated)
    }

    private fun callSimulering(oppdrag: OppdragStateDTO): SimuleringResult {
        if (oppdrag.utbetalingsOppdrag.utbetalingsLinje.isNotEmpty()) {
            return simuleringService
                    .simulerOppdrag(oppdrag.toSimuleringRequest())
        }
        return SimuleringResult(status=Status.FEIL,feilMelding = "Tomt vedtak")
    }

    fun sendUtbetalingOppdragMQ(oppdrag: OppdragStateDTO) {
        log.info("sender til Oppdragsystemet for ${oppdrag.soknadId} fagsystemId ${oppdrag.id}")
        oppdragSender.sendOppdrag(oppdrag.toOppdrag())

    }

    private fun simuleringMetrics(result: SimuleringResult) {
        if (result.status == Status.OK) {
            meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(result.mottaker!!.totalBelop.toDouble())
            meterRegistry.gauge(SIMULERING_UTBETALT_MAKS_BELOP, result.mottaker.totalBelop)
        }
        meterRegistry.counter(SIMULERING, "status", result.status.name).increment()
    }

}