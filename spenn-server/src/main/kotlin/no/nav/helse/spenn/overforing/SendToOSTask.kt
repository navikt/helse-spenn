package no.nav.helse.spenn.overforing

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.oppdrag.dao.SanityCheckException
import org.slf4j.LoggerFactory

class SendToOSTask(
    private val oppdragStateService: OppdragService,
    private val oppdragMQSender: OppdragMQSender,
    private val meterRegistry: MeterRegistry,
    private val limit: Int = 100
) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    fun sendToOS() {
        val oppdragList = oppdragStateService.hentFerdigsimulerte(limit = limit)
        if (oppdragList.size >= limit) {
            log.warn("Det ble hentet ut ${oppdragList.size} oppdrag, som er maksGrensen, kanskje klarer vi ikke å få unna alle?")
        }
        if (oppdragList.isNotEmpty()) {
            log.info("We are sending ${oppdragList.size} to OS")
        }
        oppdragList.forEach { transaksjon ->
            try {
                transaksjon.forberedSendingTilOS()
                oppdragMQSender.sendOppdrag(transaksjon.oppdragRequest)
                meterRegistry.counter(OPPDRAG, "status", TransaksjonStatus.SENDT_OS.name).increment()
            } catch (sanityError: SanityCheckException) {
                meterRegistry.counter(OPPDRAG, "status", TransaksjonStatus.STOPPET.name).increment()
                log.error("$transaksjon bestod ikke sanityCheck! Feil=${sanityError.message}. Det er derfor IKKE sendt videre til oppdragssystemet!")
                transaksjon.stopp(sanityError.message)
            }
        }

    }


}


