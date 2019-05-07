package no.nav.helse.spenn.tasks

import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.vedtak.UtbetalingService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = arrayOf("scheduler.enabled"), havingValue = "true")
class SendToOSTask(val oppdragStateService: OppdragStateService, val utbetalingService: UtbetalingService) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    @Scheduled(cron = "59 * * * * *")
    @SchedulerLock(name = "sendToOS")
    fun sendToOS() {
        log.info("running sendToOSTask() ")
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK)
        oppdragList.forEach {
            try {
                utbetalingService.sendUtbetalingOppdragMQ(it)
                it.status = OppdragStateStatus.SENDT_OS
                oppdragStateService.saveOppdragState(it)
            }
            catch(e: Exception) {
                log.error("Got exeption while sending ${it.soknadId}", e)
            }
        }
    }

}