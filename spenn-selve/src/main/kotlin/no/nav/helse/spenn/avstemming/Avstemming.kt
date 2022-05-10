package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtKø
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.OppdragDto
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Avstemming(
    private val tilAvstemming: UtKø,
    private val kafkaProducer: Producer<String, String>,
    private val rapidTopic: String,
    private val oppdragDao: OppdragDao,
    private val avstemmingDao: AvstemmingDao
) {
    private companion object {
        private val log = LoggerFactory.getLogger(Avstemming::class.java)
    }

    fun avstemTilOgMed(dagen: LocalDate) {
        val fagområder = oppdragDao.hentOppdragForAvstemming(Avstemmingsnøkkel.tilOgMed(dagen))
        if (fagområder.isEmpty()) return
        fagområder.forEach { (fagområde, oppdrag) ->
            val id = UUID.randomUUID()
            log.info("Starter avstemming id=$id fagområde=$fagområde frem til og med $dagen")
            avstemOppdrag(id, dagen, fagområde, oppdrag)
            log.info("Avstemming id=$id fagområde=$fagområde frem til og med $dagen er ferdig")
        }
    }

    private fun avstemOppdrag(
        id: UUID,
        dagen: LocalDate,
        fagområde: String,
        oppdrag: List<OppdragDto>
    ) {
        if (oppdrag.isEmpty()) return log.info("ingenting å avstemme")
        val avstemmingsperiode = OppdragDto.avstemmingsperiode(oppdrag)
        val meldinger = AvstemmingBuilder(id, fagområde, oppdrag).build()
        avstemmingDao.nyAvstemming(id, fagområde, avstemmingsperiode.endInclusive, oppdrag.size)
        log.info("avstemmer nøkkelFom=${avstemmingsperiode.start} nøkkelTom=${avstemmingsperiode.endInclusive}: sender ${meldinger.size} meldinger")
        meldinger.forEach { sendAvstemmingsmelding(it) }
        oppdragDao.oppdaterAvstemteOppdrag(fagområde, avstemmingsperiode.endInclusive)
        val avstemminginfo =
            mapOf(
                "nøkkel_fom" to avstemmingsperiode.start,
                "nøkkel_tom" to avstemmingsperiode.endInclusive,
                "antall_oppdrag" to oppdrag.size,
                "antall_avstemmingsmeldinger" to meldinger.size
            )

        kafkaProducer.send(ProducerRecord(rapidTopic, avstemmingevent(id, dagen, fagområde, oppdrag.size, avstemminginfo).toJson().also {
            log.info("sender $it")
        }))
    }

    private fun avstemmingevent(
        id: UUID,
        dagen: LocalDate,
        fagområde: String,
        antallOppdrag: Int,
        avstemminginfo: Map<String, Any>
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "avstemming",
            "@id" to id,
            "@opprettet" to LocalDateTime.now(),
            "dagen" to dagen,
            "fagområde" to fagområde,
            "antall_oppdrag" to antallOppdrag,
            "detaljer" to avstemminginfo
        )
    )

    private fun sendAvstemmingsmelding(melding: Avstemmingsdata) {
        val xmlMelding = AvstemmingdataXml.marshal(melding)
        tilAvstemming.sendNoErrorHandling(xmlMelding)
    }
}
