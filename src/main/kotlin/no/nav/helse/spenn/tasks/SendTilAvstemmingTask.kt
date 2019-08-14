package no.nav.helse.spenn.tasks

import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.SchedulerLock
import no.nav.helse.spenn.FagOmraadekode
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.grensesnittavstemming.AvstemmingMapper
import no.nav.helse.spenn.metrics.AVSTEMMING
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
@Profile(value=["!prod"])
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.avstemming"], havingValue = "true")
class SendTilAvstemmingTask(val oppdragStateService: OppdragStateService,
                            val avstemmingMQSender: AvstemmingMQSender,
                            val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(SendTilAvstemmingTask::class.java)

    @Scheduled(cron = "0 0 13 * * *")
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
        try {
            meldinger[1].grunnlag.apply {
                meterRegistry.counter(AVSTEMMING, "type", "godkjent").increment(this.godkjentAntall.toDouble())
                meterRegistry.counter(AVSTEMMING, "type", "avvist").increment(this.avvistAntall.toDouble())
                meterRegistry.counter(AVSTEMMING, "type", "mangler").increment(this.manglerAntall.toDouble())
                meterRegistry.counter(AVSTEMMING, "type", "varsel").increment(this.varselAntall.toDouble())
            }
        } catch (e : Exception) {
            log.error("Error registering metrics", e)
        }


        oppdragList.forEach {
            val newState = it.copy(avstemming = it.avstemming!!.copy(avstemt = true))
            oppdragStateService.saveOppdragState(newState)
        }
    }
}


