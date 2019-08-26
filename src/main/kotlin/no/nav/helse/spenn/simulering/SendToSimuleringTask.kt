package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.appsupport.SIMULERING
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct

@Component
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.simulering"], havingValue = "true")
class SendToSimuleringTask(val simuleringService: SimuleringService,
                           val oppdragStateService: OppdragStateService,
                           val meterRegistry: MeterRegistry,
                           @Value("\${scheduler.tasks.simulering.limit:100}") val limit: Int = 100) {

    private val maksBelopGauge = AtomicLong(0)

    companion object {
        private val LOG = LoggerFactory.getLogger(SendToSimuleringTask::class.java)
    }

    @PostConstruct
    fun init() {
        LOG.info("init gauge maksBeløp")
        meterRegistry.gauge(SIMULERING_UTBETALT_MAKS_BELOP, maksBelopGauge)
    }
    
    @Scheduled(cron = "30 * 7-21 * * ?")
    @SchedulerLock(name = "sendToSimulering")
    fun sendSimulering() {
        LOG.info("Running SendToSimulering task")
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.STARTET, limit)
        LOG.info("Got ${oppdragList.size} items for simulering")
        oppdragList.forEach {
            val updated = oppdragStateService.saveOppdragState(simuleringService.runSimulering(it))
            simuleringMetrics(updated)
        }
    }

    private fun simuleringMetrics(oppdrag: OppdragStateDTO) {
        if (oppdrag.simuleringResult != null) {
            if (oppdrag.simuleringResult.status == Status.OK) {
                meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(oppdrag.simuleringResult.mottaker!!.totalBelop.toDouble())
                maksBelopGauge.set(oppdrag.simuleringResult.mottaker.totalBelop.toLong())
            }
            meterRegistry.counter(SIMULERING, "status", oppdrag.simuleringResult.status.name).increment()
        }
    }
}