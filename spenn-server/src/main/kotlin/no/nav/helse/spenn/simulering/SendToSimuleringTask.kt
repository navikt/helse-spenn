package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.Metrics.countSimulering
import no.nav.helse.spenn.Metrics.countUtbetaltBeløp
import no.nav.helse.spenn.Metrics.setMaksbeløpGauge
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.slf4j.LoggerFactory

class SendToSimuleringTask(
    private val simuleringService: SimuleringService,
    private val oppdragService: OppdragService,
    private val limit: Int = 100
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SendToSimuleringTask::class.java)
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
            countUtbetaltBeløp(simulering.totalBelop)
            setMaksbeløpGauge(simulering.totalBelop)
        }
        countSimulering(simuleringResult.status)
    }
}
