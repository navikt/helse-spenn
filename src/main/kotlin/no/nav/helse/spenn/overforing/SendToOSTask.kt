package no.nav.helse.spenn.overforing

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal


@Component
@Profile(value=["!prod"])
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.oppdrag"], havingValue = "true")
class SendToOSTask(val oppdragStateService: OppdragStateService,
                   val utbetalingService: UtbetalingService,
                   val meterRegistry: MeterRegistry,
                   @Value("\${scheduler.tasks.oppdrag.limit:100}") val limit: Int = 100) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    @Scheduled(cron = "59 * * * * *")
    @SchedulerLock(name = "sendToOS")
    fun sendToOS() {
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK,limit)
        if (oppdragList.size >= limit) {
            log.warn("Det ble hentet ut ${oppdragList.size} oppdrag, som er maksGrensen, kanskje klarer vi ikke å få unna alle?")
        }
        log.info("We are sending ${oppdragList.size} to OS")

        oppdragList.forEach {
            if (passesSanityCheck(it)) {
                val updated = it.copy(status = OppdragStateStatus.SENDT_OS, avstemming = AvstemmingDTO())
                oppdragStateService.saveOppdragState(updated)
                utbetalingService.sendUtbetalingOppdragMQ(updated)
                meterRegistry.counter(OPPDRAG, "status", OppdragStateStatus.SENDT_OS.name).increment()
            } else {
                meterRegistry.counter(OPPDRAG, "status", "INSANE").increment()
                log.error("oppdrag med soknadId=${it.soknadId} bestod ikke sanityCheck!")
            }
        }

    }

    fun passesSanityCheck(oppdragDTO: OppdragStateDTO) : Boolean {
        val maksDagsats = BigDecimal(99858 * 6.5 / 260) // TODO ?
        oppdragDTO.utbetalingsOppdrag.utbetalingsLinje.forEach {
            if (it.sats > maksDagsats) {
                log.error("sats i oppdrag med soknadId=${oppdragDTO.soknadId} er ${it.sats} som er høyere enn begrensningen på $maksDagsats")
                return false
            }
        }
        return true
    }

}


