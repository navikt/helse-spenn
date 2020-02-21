package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.ibm.mq.jms.MQQueue
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.appsupport.OPPDRAG
import no.nav.helse.spenn.core.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.core.avstemmingsnokkelFormatter
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.jms.Connection

class OppdragMQReceiver(
    connection: Connection, // NB: It is the responsibility of the caller to call connection.start()
    mottakqueue: String,
    val rapidsConnection: RapidsConnection,
    val jaxb: JAXBOppdrag,
    val oppdragService: OppdragService,
    val meterRegistry: MeterRegistry/*,
                        val statusProducer: OppdragStateKafkaProducer*/
) {

    private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

    private val jmsSession = connection.createSession()
    private val consumer = jmsSession
        .createConsumer(MQQueue(mottakqueue))

    init {
        consumer.setMessageListener { m ->
            val body = m.getBody(String::class.java)
            try {
                receiveOppdragResponse(body)
            } catch (e: Exception) {
                log.error(e.message, e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppdragMQReceiver::class.java)

        internal fun mapStatus(oppdrag: Oppdrag): TransaksjonStatus =
            when (KvitteringAlvorlighetsgrad.fromKode(oppdrag.mmel.alvorlighetsgrad)) {
                KvitteringAlvorlighetsgrad.OK -> TransaksjonStatus.FERDIG
                KvitteringAlvorlighetsgrad.AKSEPTERT_MEN_NOE_ER_FEIL -> {
                    log.warn("Akseptert men noe er feil for ${oppdrag.oppdrag110.fagsystemId} melding ${oppdrag.mmel.beskrMelding}")
                    TransaksjonStatus.FERDIG
                }
                else -> {
                    log.error("FEIL for oppdrag ${oppdrag.oppdrag110.fagsystemId} ${oppdrag.mmel.beskrMelding}")
                    TransaksjonStatus.FEIL
                }
            }

    }

    //@JmsListener(destination = "\${oppdrag.queue.mottak}")
    fun receiveOppdragResponse(response: String) {
        log.trace(response)
        //rar xml som blir returnert
        val replaced = response.replace("oppdrag xmlns", "ns2:oppdrag xmlns:ns2")
        handleResponse(jaxb.toOppdrag(replaced), replaced)
    }

    private fun handleResponse(oppdrag: Oppdrag, xml: String) {
        require(oppdrag.oppdrag110.fagsystemId != null)
        val utbetalingsreferanse = oppdrag.oppdrag110.fagsystemId
        log.info("OppdragResponse for $utbetalingsreferanse  ${oppdrag.mmel.alvorlighetsgrad}  ${oppdrag.mmel.beskrMelding}")
        require(oppdrag.oppdrag110.avstemming115.nokkelAvstemming != null)
        val nøkkelAvstemming =
            LocalDateTime.parse(oppdrag.oppdrag110.avstemming115.nokkelAvstemming, avstemmingsnokkelFormatter)
        val status = mapStatus(oppdrag)
        val feilmld = if (status == TransaksjonStatus.FEIL) oppdrag.mmel.beskrMelding else null

        val transaksjon = oppdragService.hentTransaksjon(utbetalingsreferanse, nøkkelAvstemming)

        transaksjon.lagreOSResponse(status, xml, feilmld)

        meterRegistry.counter(OPPDRAG, "status", status.name).increment()
        val opprinneligBehovAsJsonNode = defaultObjectMapper.readTree(transaksjon.opprinneligBehov())

        rapidsConnection.publish(transaksjon.gjelderId(), opprinneligBehovAsJsonNode.setLøsning(
            "Utbetaling",
            mapOf(
                "status" to status,
                "melding" to (oppdrag.mmel.beskrMelding ?: "")
            )
        ).toString())

    }

    fun close() {
        log.info("Closing OppdragMQReceiver::consumer")
        consumer.close()
        log.info("Closing OppdragMQReceiver::jmsSession")
        jmsSession.close()
    }

    private fun JsonNode.setLøsning(nøkkel: String, data: Any) =
        (this as ObjectNode).set<JsonNode>(
            "@løsning", defaultObjectMapper.convertValue(
            mapOf(
                nøkkel to data
            ))
        )

}
