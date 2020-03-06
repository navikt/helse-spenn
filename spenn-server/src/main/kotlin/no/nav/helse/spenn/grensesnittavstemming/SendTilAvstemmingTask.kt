package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.Metrics.tellAvstemming
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.oppdrag.dao.lagAvstemmingsmeldinger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SendTilAvstemmingTask(
    private val oppdragStateService: OppdragService,
    private val avstemmingMQSender: AvstemmingMQSender,
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
                tellAvstemming(this)
            }
        } catch (e: Exception) {
            log.error("Error registering metrics", e)
        }

        oppdragList.forEach {
            it.markerSomAvstemt()
        }
    }
}


