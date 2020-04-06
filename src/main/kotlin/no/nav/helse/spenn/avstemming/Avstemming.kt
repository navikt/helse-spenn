package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.OppdragDto
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.jms.Connection

internal class Avstemming(
    connection: Connection,
    sendQueue: String,
    private val kafkaProducer: Producer<String, String>,
    private val rapidTopic: String,
    private val oppdragDao: OppdragDao,
    private val avstemmingDao: AvstemmingDao
) {
    private companion object {
        private val log = LoggerFactory.getLogger(Avstemming::class.java)
    }
    private val jmsSession = connection.createSession()
    private val jmsProducer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    fun avstem(id: UUID, dagen: LocalDate) {
        val avstemmingsperiodeForDag = Avstemmingsnøkkel.periode(dagen)
        val oppdrag = oppdragDao.hentOppdragForAvstemming(avstemmingsperiodeForDag.endInclusive)
        val event = avstemmingevent(id, dagen, oppdrag.size)
        avstemOppdrag(id, oppdrag, event)
        kafkaProducer.send(ProducerRecord(rapidTopic, event.toJson().also { log.info("sender $it") }))
            .get()
    }

    private fun avstemOppdrag(id: UUID, oppdrag: List<OppdragDto>, event: JsonMessage) {
        if (oppdrag.isEmpty()) return log.info("ingenting å avstemme")
        val avstemmingsperiode = OppdragDto.avstemmingsperiode(oppdrag)
        val meldinger = AvstemmingBuilder(id, oppdrag).build()
        avstemmingDao.nyAvstemming(id, avstemmingsperiode.endInclusive, oppdrag.size)
        log.info("avstemmer nøkkelFom=${avstemmingsperiode.start} nøkkelTom=${avstemmingsperiode.endInclusive}: sender ${meldinger.size} meldinger")
        meldinger.forEach { sendAvstemmingsmelding(it) }
        oppdragDao.oppdaterAvstemteOppdrag(avstemmingsperiode.endInclusive)
        event["nøkkel_fom"] = avstemmingsperiode.start
        event["nøkkel_tom"] = avstemmingsperiode.endInclusive
        event["antall_avstemmingsmeldinger"] = meldinger.size
    }

    private fun avstemmingevent(id: UUID, dagen: LocalDate, antallOppdrag: Int) = JsonMessage.newMessage(mapOf(
        "@event_name" to "avstemming",
        "@id" to id,
        "@opprettet" to LocalDateTime.now(),
        "dagen" to dagen,
        "antall_oppdrag" to antallOppdrag
    ))

    private fun sendAvstemmingsmelding(melding: Avstemmingsdata) {
        val xmlMelding = AvstemmingdataXml.marshal(melding)
        jmsProducer.send(jmsSession.createTextMessage(xmlMelding))
    }
}
