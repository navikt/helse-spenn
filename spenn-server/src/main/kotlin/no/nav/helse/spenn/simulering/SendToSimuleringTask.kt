package no.nav.helse.spenn.simulering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.SIMULERING
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_BELOP
import no.nav.helse.spenn.appsupport.SIMULERING_UTBETALT_MAKS_BELOP
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct

class SendToSimuleringTask(
    private val simuleringService: SimuleringService,
    private val oppdragService: OppdragService,
    private val meterRegistry: MeterRegistry,
    private val limit: Int = 100
) {
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
            meterRegistry.counter(SIMULERING_UTBETALT_BELOP).increment(simulering.totalBelop.toDouble())
            maksBelopGauge.set(simulering.totalBelop.toLong())
        }
        meterRegistry.counter(SIMULERING, "status", simuleringResult.status.name).increment()
    }
}
