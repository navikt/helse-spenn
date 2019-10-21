package no.nav.helse.spenn.oppdrag

import com.ibm.mq.jms.MQQueue
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spenn.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import javax.jms.Connection

/*import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component*/

//@Component
class OppdragMQReceiver(connection: Connection, // NB: It is the responsibility of the caller to call connection.start()
                        mottakqueue: String, /*@Value("\${oppdrag.queue.mottak}")*/
                        val jaxb: JAXBOppdrag,
                        val oppdragStateService: OppdragStateService,
                        val meterRegistry: MeterRegistry/*,
                        val statusProducer: OppdragStateKafkaProducer*/) {

    private val jmsSession = connection.createSession()
    private val consumer = jmsSession
            .createConsumer(MQQueue(mottakqueue))

    init {
        consumer.setMessageListener { m ->
            val body = m.getBody(String::class.java)
            val ignored = receiveOppdragResponse(body)
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
    fun receiveOppdragResponse(response: String): OppdragStateDTO {
        log.debug(response)
        //rar xml som blir returnert
        val replaced = response.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        return handleResponse(jaxb.toOppdrag(replaced), replaced)
    }

    private fun handleResponse(oppdrag: Oppdrag, xml: String): OppdragStateDTO {
        val uuid = oppdrag.oppdrag110.fagsystemId.fromFagId()
        log.info("OppdragResponse for ${uuid} ${oppdrag.oppdrag110.fagsystemId}  ${oppdrag.mmel.alvorlighetsgrad}  ${oppdrag.mmel.beskrMelding}")
        val state = oppdragStateService.fetchOppdragState(uuid)
        val status = mapStatus(oppdrag)
        val feilmld = if (status == OppdragStateStatus.FEIL) oppdrag.mmel.beskrMelding else null
        val updated = state.copy(oppdragResponse = xml, status = status, feilbeskrivelse = feilmld)
        meterRegistry.counter(OPPDRAG, "status", updated.status.name).increment()
        if (updated.status == OppdragStateStatus.FERDIG) {
            //statusProducer.send(OppdragFerdigInfo(updated.soknadId.toString())) // TODO
        }
        return oppdragStateService.saveOppdragState(updated)
    }

    fun close() {
        log.info("Closing OppdragMQReceiver::consumer")
        consumer.close()
        log.info("Closing OppdragMQReceiver::jmsSession")
        jmsSession.close()
    }
}