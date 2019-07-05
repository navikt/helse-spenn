package no.nav.helse.spenn.tasks

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.metrics.SENDT_TIL_OS
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.vedtak.UtbetalingService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.oppdrag"], havingValue = "true")
class SendToOSTask(val oppdragStateService: OppdragStateService,
                   val utbetalingService: UtbetalingService,
                   val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    @Scheduled(cron = "59 * * * * *")
    @SchedulerLock(name = "sendToOS")
    fun sendToOS() {
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK)
        log.info("We are sending ${oppdragList.size} to OS")

        oppdragList.forEach {
            val updated = it.copy(status = OppdragStateStatus.SENDT_OS, avstemming = AvstemmingDTO())
            oppdragStateService.saveOppdragState(updated)
            utbetalingService.sendUtbetalingOppdragMQ(updated)
            meterRegistry.counter(SENDT_TIL_OS,"status","ok").increment()

        }

    }

}


