package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.appsupport.SIMULERING
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.metrics
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class SendToSimuleringTask(
    private val simuleringService: SimuleringService,
    private val oppdragService: OppdragService,
    private val limit: Int = 100
) {
    private val maksBelopGauge = AtomicLong(0)

    companion object {
        private val LOG = LoggerFactory.getLogger(SendToSimuleringTask::class.java)
    }

    init {
        LOG.info("init gauge maksBeløp")
        metrics.gauge(SIMULERING_UTBETALT_MAKS_BELOP, maksBelopGauge)
    }

    fun sendSimulering() {
        LOG.info("Running SendToSimulering task")
        val oppdragList = oppdragService.hentNyeOppdrag(limit)
        if (oppdragList.isNotEmpty()) {
            LOG.info("Got ${oppdragList.size} items for simulering")
        }
        oppdragList.forEach { transaksjon ->
            val result = simuleringService.runSimulering(transaksjon)

            transaksjon.oppdaterSimuleringsresultat(result)
            simuleringMetrics(result)
        }
    }

    private fun simuleringMetrics(simuleringResult: SimuleringResult) {
        val simulering = simuleringResult.simulering
        if (simuleringResult.status == SimuleringStatus.OK && simulering != null) {
            metrics.counter(SIMULERING_UTBETALT_BELOP).increment(simulering.totalBelop.toDouble())
            maksBelopGauge.set(simulering.totalBelop.toLong())
        }
        metrics.counter(SIMULERING, "status", simuleringResult.status.name).increment()
    }
}
