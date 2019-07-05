package no.nav.helse.spenn.tasks

import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.FagOmraadekode
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.grensesnittavstemming.AvstemmingMapper
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.avstemming"], havingValue = "true")
class SendTilAvstemmingTask(val oppdragStateService: OppdragStateService,
                        val avstemmingMQSender: AvstemmingMQSender) {

    private val log = LoggerFactory.getLogger(SendTilAvstemmingTask::class.java)

    @Scheduled(cron = "0 0 21 * * *")
    @SchedulerLock(name = "sendTilAvstemming")
    fun sendTilAvstemming() {
        val oppdragList = oppdragStateService.fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(LocalDateTime.now().minusHours(1))
        log.info("Fant ${oppdragList.size} oppdrag som skal sendes til avstemming")
        val mapper = AvstemmingMapper(oppdragList, FagOmraadekode.SYKEPENGER_REFUSJON)
        val meldinger = mapper.lagAvstemmingsMeldinger()

        if (meldinger.isEmpty()) {
            log.info("Ingen avstemmingsmeldinger å sende. Returnerer...")
            return
        }
        log.info("Sender avstemmingsmeldinger med avleverendeAvstemmingId=${meldinger.first().aksjon.avleverendeAvstemmingId}, "+
                "nokkelFom=${mapper.avstemmingsnokkelFom()}, nokkelTom=${mapper.avstemmingsnokkelTom()}")

        meldinger.forEachIndexed { i, melding ->
            try {
                avstemmingMQSender.sendAvstemmingsmelding(melding)
            }
            catch(e: Exception) {
                log.error("Got exeption while sending message ${i+1} of ${meldinger.size}, having aksjonsType=${melding.aksjon.aksjonType.value()}. Cancelling and returning" , e)
                return
            }
        }

        oppdragList.forEach {
            val newState = it.copy(avstemming = it.avstemming!!.copy(avstemt = true))
            oppdragStateService.saveOppdragState(newState)
        }
    }
}


