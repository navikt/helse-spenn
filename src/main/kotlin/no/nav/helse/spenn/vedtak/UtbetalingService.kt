package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.oppdrag.OppdragMQSender
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.toOppdrag
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service

@Service
class UtbetalingService(val oppdragSender: OppdragMQSender) {
    companion object {
        private val log = LoggerFactory.getLogger(UtbetalingService::class.java)
    }

    fun sendUtbetalingOppdragMQ(oppdrag: OppdragStateDTO) {
        log.info("sender til Oppdragsystemet for ${oppdrag.soknadId} fagsystemId ${oppdrag.id}")
        oppdragSender.sendOppdrag(oppdrag.toOppdrag())
    }

}