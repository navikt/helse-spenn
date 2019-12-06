package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.SIMULERING
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.oppdrag.TransaksjonDTO
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
            val result = simuleringService.runSimulering(it)
            val status = when (result.status) {
                Status.OK -> OppdragStateStatus.SIMULERING_OK
                else -> OppdragStateStatus.SIMULERING_FEIL
            }
            //return oppdrag.copy(simuleringResult = result, status = status)
            oppdragStateService.oppdaterSimuleringsresultat(it, result, status)
            //val updated = oppdragStateService.saveOppdragState(simuleringService.runSimulering(it))
            simuleringMetrics(result)
        }
    }

    private fun simuleringMetrics(simuleringResult: SimuleringResult) {
        if (simuleringResult != null) {
            if (simuleringResult.status == Status.OK) {
                meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(simuleringResult.simulering!!.totalBelop.toDouble())
                maksBelopGauge.set(simuleringResult.simulering.totalBelop.toLong())
            }
            meterRegistry.counter(SIMULERING, "status", simuleringResult.status.name).increment()
        }
    }
}