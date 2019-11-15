package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.SIMULERING
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct

class SendToSimuleringTask(val simuleringService: SimuleringService,
                           val oppdragStateService: OppdragStateService,
                           val meterRegistry: MeterRegistry,
                           val limit: Int = 100) {

    private val maksBelopGauge = AtomicLong(0)

    companion object {
        private val LOG = LoggerFactory.getLogger(SendToSimuleringTask::class.java)
    }

    @PostConstruct
    fun init() {
        LOG.info("init gauge maksBeløp")
        meterRegistry.gauge(SIMULERING_UTBETALT_MAKS_BELOP, maksBelopGauge)
    }

    fun sendSimulering() {
        LOG.trace("Running SendToSimulering task")
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.STARTET, limit)
        if (oppdragList.isNotEmpty()) {
            LOG.info("Got ${oppdragList.size} items for simulering")
        }
        oppdragList.forEach {
            val updated = oppdragStateService.saveOppdragState(simuleringService.runSimulering(it))
            simuleringMetrics(updated)
        }
    }

    private fun simuleringMetrics(oppdrag: OppdragStateDTO) {
        if (oppdrag.simuleringResult != null) {
            if (oppdrag.simuleringResult.status == Status.OK) {
                meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(oppdrag.simuleringResult.simulering!!.totalBelop.toDouble())
                maksBelopGauge.set(oppdrag.simuleringResult.simulering.totalBelop.toLong())
            }
            meterRegistry.counter(SIMULERING, "status", oppdrag.simuleringResult.status.name).increment()
        }
    }
}