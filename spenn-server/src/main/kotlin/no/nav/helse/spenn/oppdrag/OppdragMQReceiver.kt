package no.nav.helse.spenn.oppdrag

import com.ibm.mq.jms.MQQueue
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.avstemmingsnokkelFormatter
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.jms.Connection

class OppdragMQReceiver(connection: Connection, // NB: It is the responsibility of the caller to call connection.start()
                        mottakqueue: String,
                        val jaxb: JAXBOppdrag,
                        val oppdragStateService: OppdragStateService,
                        val meterRegistry: MeterRegistry/*,
                        val statusProducer: OppdragStateKafkaProducer*/) {

    private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

    private val jmsSession = connection.createSession()
    private val consumer = jmsSession
            .createConsumer(MQQueue(mottakqueue))

    init {
        consumer.setMessageListener { m ->
            val body = m.getBody(String::class.java)
            try {
                val ignored = receiveOppdragResponse(body)
            } catch (e: Exception) {
                log.error(e.message, e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

        internal fun mapStatus(oppdrag: Oppdrag): OppdragStateStatus {
            when (KvitteringAlvorlighetsgrad.fromKode(oppdrag.mmel.alvorlighetsgrad)) {
                KvitteringAlvorlighetsgrad.OK -> return OppdragStateStatus.FERDIG
                KvitteringAlvorlighetsgrad.AKSEPTERT_MEN_NOE_ER_FEIL -> {
                    log.warn("Akseptert men noe er feil for ${oppdrag.oppdrag110.fagsystemId} melding ${oppdrag.mmel.beskrMelding}")
                    return OppdragStateStatus.FERDIG
                }
            }
            log.error("FEIL for oppdrag ${oppdrag.oppdrag110.fagsystemId} ${oppdrag.mmel.beskrMelding}")
            return OppdragStateStatus.FEIL
        }
    }

    //@JmsListener(destination = "\${oppdrag.queue.mottak}")
    fun receiveOppdragResponse(response: String) {
        log.debug(response)
        //rar xml som blir returnert
        val replaced = response.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        handleResponse(jaxb.toOppdrag(replaced), replaced)
    }

    private fun handleResponse(oppdrag: Oppdrag, xml: String) {
        val utbetalingsreferanse = oppdrag.oppdrag110.fagsystemId!!
        log.info("OppdragResponse for ${utbetalingsreferanse}  ${oppdrag.mmel.alvorlighetsgrad}  ${oppdrag.mmel.beskrMelding}")
        val nøkkelAvstemming = LocalDateTime.parse(oppdrag.oppdrag110.avstemming115.nokkelAvstemming!!, avstemmingsnokkelFormatter)
        val status = mapStatus(oppdrag)
        val feilmld = if (status == OppdragStateStatus.FEIL) oppdrag.mmel.beskrMelding else null

        oppdragStateService.lagreOSResponse(utbetalingsreferanse, nøkkelAvstemming, status, xml, feilmld)

        meterRegistry.counter(OPPDRAG, "status", status.name).increment()
        if (status == OppdragStateStatus.FERDIG) {
            //statusProducer.send(OppdragFerdigInfo(updated.soknadId.toString())) // TODO
        }
    }

    fun close() {
        log.info("Closing OppdragMQReceiver::consumer")
        consumer.close()
        log.info("Closing OppdragMQReceiver::jmsSession")
        jmsSession.close()
    }
}