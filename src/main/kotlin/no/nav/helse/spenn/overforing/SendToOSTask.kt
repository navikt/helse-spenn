package no.nav.helse.spenn.overforing

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import org.slf4j.LoggerFactory
import java.math.BigDecimal

/*@Profile(value=["!prod"])
@ConditionalOnProperty(name = ["scheduler.enabled", "scheduler.tasks.oppdrag"], havingValue = "true")*/
class SendToOSTask(val oppdragStateService: OppdragStateService,
                   val utbetalingService: UtbetalingService,
                   val meterRegistry: MeterRegistry,
                   /*@Value("\${scheduler.tasks.oppdrag.limit:100}")*/ val limit: Int = 100) {

    private val log = LoggerFactory.getLogger(SendToOSTask::class.java)

    //@Scheduled(cron = "59 * * * * *")
    //@SchedulerLock(name = "sendToOS")
    fun sendToOS() {
        val oppdragList = oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_OK,limit)
        if (oppdragList.size >= limit) {
            log.warn("Det ble hentet ut ${oppdragList.size} oppdrag, som er maksGrensen, kanskje klarer vi ikke å få unna alle?")
        }
        log.info("We are sending ${oppdragList.size} to OS")

        oppdragList.forEach {
            try {
                performSanityCheck(it)
                val updated = it.copy(status = OppdragStateStatus.SENDT_OS, avstemming = AvstemmingDTO())
                oppdragStateService.saveOppdragState(updated)
                utbetalingService.sendUtbetalingOppdragMQ(updated)
                meterRegistry.counter(OPPDRAG, "status", OppdragStateStatus.SENDT_OS.name).increment()
            } catch (sanityError: SanityCheckException) {
                meterRegistry.counter(OPPDRAG, "status", OppdragStateStatus.STOPPET.name).increment()
                log.error("Oppdrag med soknadId=${it.sakskompleksId} bestod ikke sanityCheck! Feil=${sanityError.message}. Det er derfor IKKE sendt videre til oppdragssystemet!")
                val updated = it.copy(status = OppdragStateStatus.STOPPET, feilbeskrivelse = sanityError.message)
                oppdragStateService.saveOppdragState(updated)
            }
        }

    }

    fun performSanityCheck(oppdragDTO: OppdragStateDTO) {
        oppdragDTO.utbetalingsOppdrag.utbetalingsLinje.forEach {
            if (it.satsTypeKode != SatsTypeKode.DAGLIG) {
                throw SanityCheckException("satsTypeKode er ${it.satsTypeKode}. Vi har ikke logikk for å sanity-sjekke dette.")
            }
            val maksDagsats = maksTillattDagsats()
            if (it.sats > maksDagsats) {
                throw SanityCheckException("dagsats ${it.sats} som er høyere enn begrensningen på $maksDagsats")
            }
        }
    }

    private fun maksTillattDagsats() : BigDecimal {
        // TODO ? Konfig? Sykepenger er maks 6G, maksTillattDagsats kan ikke være lavere enn dette.
        val G = 100000
        val hverdagerPerAar = 260
        return BigDecimal(6.5 * G / hverdagerPerAar)
    }

    private class SanityCheckException(message : String) : Exception(message)

}


