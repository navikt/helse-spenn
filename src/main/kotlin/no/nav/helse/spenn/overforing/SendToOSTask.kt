package no.nav.helse.spenn.overforing

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.TransaksjonDTO
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

class SendToOSTask(private val oppdragStateService: OppdragStateService,
                   private val oppdragMQSender: OppdragMQSender,
                   private val meterRegistry: MeterRegistry,
                   private val limit: Int = 100) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    fun sendToOS() {
        val oppdragList = oppdragStateService.hentFerdigsimulerte(limit = limit)
        if (oppdragList.size >= limit) {
            log.warn("Det ble hentet ut ${oppdragList.size} oppdrag, som er maksGrensen, kanskje klarer vi ikke å få unna alle?")
        }
        if (oppdragList.isNotEmpty()) {
            log.info("We are sending ${oppdragList.size} to OS")
        }
        oppdragList.forEach {transaksjon ->
            try {
                performSanityCheck(transaksjon)
                transaksjon.forberedSendingTilOS()
                //val updated = it.copy(status = TransaksjonStatus.SENDT_OS, nokkel = LocalDateTime.now())
                //val updated = oppdragStateService.forberedSendingTilOS(it)
                oppdragMQSender.sendOppdrag(transaksjon.getOppdragRequest())
                meterRegistry.counter(OPPDRAG, "status", OppdragStateStatus.SENDT_OS.name).increment()
            } catch (sanityError: SanityCheckException) {
                meterRegistry.counter(OPPDRAG, "status", OppdragStateStatus.STOPPET.name).increment()
                log.error("$transaksjon bestod ikke sanityCheck! Feil=${sanityError.message}. Det er derfor IKKE sendt videre til oppdragssystemet!")
                //val updated = it.copy(status = OppdragStateStatus.STOPPET, feilbeskrivelse = sanityError.message)
                //oppdragStateService.saveOppdragState(updated)
                transaksjon.stopp(sanityError.message)
            }
        }

    }


}


