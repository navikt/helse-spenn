package no.nav.helse.spenn.grensesnittavstemming

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.AVSTEMMING
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.oppdrag.dao.lagAvstemmingsmeldinger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SendTilAvstemmingTask(
    private val oppdragStateService: OppdragService,
    private val avstemmingMQSender: AvstemmingMQSender,
    private val meterRegistry: MeterRegistry,
    private val marginInHours: Long = 1L
) {

    private val log = LoggerFactory.getLogger(SendTilAvstemmingTask::class.java)

    fun sendTilAvstemming() {
        val oppdragList =
            oppdragStateService.hentEnnåIkkeAvstemteTransaksjonerEldreEnn(LocalDateTime.now().minusHours(marginInHours))
        log.info("Fant ${oppdragList.size} oppdrag som skal sendes til avstemming")
        val meldinger = oppdragList.lagAvstemmingsmeldinger()

        if (meldinger.isEmpty()) {
            log.info("Ingen avstemmingsmeldinger å sende. Returnerer...")
            return
        }
        log.info("Sender avstemmingsmeldinger med avleverendeAvstemmingId=${meldinger.first().aksjon.avleverendeAvstemmingId}")

        meldinger.forEachIndexed { i, melding ->
            try {
                avstemmingMQSender.sendAvstemmingsmelding(melding)
            } catch (e: Exception) {
                log.error(
                    "Got exeption while sending message ${i + 1} of ${meldinger.size}, having aksjonsType=${melding.aksjon.aksjonType.value()}. Cancelling and returning",
                    e
                )
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
        } catch (e: Exception) {
            log.error("Error registering metrics", e)
        }

        oppdragList.forEach {
            it.markerSomAvstemt()
        }
    }
}


