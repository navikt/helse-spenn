package no.nav.helse.spenn.tasks

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.metrics.SIMULERING
import no.nav.helse.spenn.metrics.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.metrics.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.vedtak.UtbetalingService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.simulering"], havingValue = "true")
class SendToSimuleringTask(val utbetalingService: UtbetalingService,
                       val oppdragStateService: OppdragStateService,
                       val meterRegistry: MeterRegistry) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendToSimuleringTask::class.java)
    }

    @Scheduled(cron = "30 * * * * *")
    @SchedulerLock(name = "sendToSimulering")
    fun sendSimulering() {
        LOG.info("Running SendToSimulering task")
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.STARTET)
        LOG.info("Got ${oppdragList.size} items for simulering")
        oppdragList.forEach {
            val updated = oppdragStateService.saveOppdragState(utbetalingService.runSimulering(it))
            simuleringMetrics(updated)
        }

    }

    private fun simuleringMetrics(oppdrag: OppdragStateDTO) {
        if (oppdrag.simuleringResult != null) {
            if (oppdrag.simuleringResult.status == Status.OK) {
                meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(oppdrag.simuleringResult.mottaker!!.totalBelop.toDouble())
                meterRegistry.gauge(SIMULERING_UTBETALT_MAKS_BELOP, oppdrag.simuleringResult.mottaker.totalBelop)
            }
            meterRegistry.counter(SIMULERING, "status", oppdrag.simuleringResult.status.name).increment()
        }
    }

}