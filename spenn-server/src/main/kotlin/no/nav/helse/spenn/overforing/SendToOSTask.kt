package no.nav.helse.spenn.overforing

import no.nav.helse.spenn.Metrics.tellOppdragSendt
import no.nav.helse.spenn.Metrics.tellOppdragStoppet
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.oppdrag.dao.SanityCheckException
import org.slf4j.LoggerFactory

class SendToOSTask(
    private val oppdragStateService: OppdragService,
    private val oppdragMQSender: OppdragMQSender,
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
                tellOppdragSendt()
            } catch (sanityError: SanityCheckException) {
                tellOppdragStoppet()
                log.error("$transaksjon bestod ikke sanityCheck! Feil=${sanityError.message}. Det er derfor IKKE sendt videre til oppdragssystemet!")
                transaksjon.stopp(sanityError.message)
            }
        }

    }
}


