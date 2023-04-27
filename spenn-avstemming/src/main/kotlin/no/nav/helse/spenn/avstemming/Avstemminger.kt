package no.nav.helse.spenn.avstemming

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.log

internal class Avstemminger(
    rapidsConnection: RapidsConnection,
    oppdragDao: OppdragDao,
    avstemmingDao: AvstemmingDao,
    tilAvstemming: UtKø,
) {
    private companion object {
        private val logger = Logg.ny(Avstemminger::class)
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "utfør_avstemming")
                it.require("dagen", JsonNode::asLocalDate)
            }
        }.register(UtførAvstemming(oppdragDao, avstemmingDao, tilAvstemming))
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "avstemming")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireKey("detaljer.nøkkel_tom")
                it.requireKey("detaljer.antall_oppdrag")
            }
        }.register(UtførteAvstemminger(oppdragDao))
    }

    private class UtførAvstemming(
        private val oppdragDao: OppdragDao,
        private val avstemmingDao: AvstemmingDao,
        private val utkø: UtKø
    ) : River.PacketListener {
        override fun onError(problems: MessageProblems, context: MessageContext) {
            logger
                .offentligError("Forstod ikke utfør_avstemming (se sikkerlogg for detaljer)")
                .privatError("Forstod ikke utfør_avstemming:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val dagen = packet["dagen"].asLocalDate()
            val avstemmingsnøkkelTom = Avstemmingsnøkkel.tilOgMed(dagen)
            logger.info("forbereder avstemming av $dagen frem til og med avstemmingsnøkkel=$avstemmingsnøkkelTom")

            val fagområder = oppdragDao.hentOppdragForAvstemming(avstemmingsnøkkelTom)
            if (fagområder.isEmpty()) {
                logger.info("det var ingenting å avstemme")
                return
            }
            fagområder.forEach { (fagområde, oppdrag) ->
                val id = UUID.randomUUID()
                logger.info("Starter avstemming id=$id fagområde=$fagområde frem til og med $dagen")
                avstemOppdrag(context, id, dagen, fagområde, oppdrag)
                logger.info("Avstemming id=$id fagområde=$fagområde frem til og med $dagen er ferdig")
            }
        }

        private fun avstemOppdrag(
            context: MessageContext,
            id: UUID,
            dagen: LocalDate,
            fagområde: String,
            oppdrag: List<OppdragDto>
        ) {
            if (oppdrag.isEmpty()) {
                logger.info("ingenting å avstemme for fagområde $fagområde")
                return
            }
            val avstemmingsperiode = OppdragDto.avstemmingsperiode(oppdrag)
            val meldinger = AvstemmingBuilder(id, fagområde, oppdrag).build()
            avstemmingDao.nyAvstemming(id, fagområde, avstemmingsperiode.endInclusive, oppdrag.size)
            logger.info("avstemmer nøkkelFom=${avstemmingsperiode.start} nøkkelTom=${avstemmingsperiode.endInclusive}: sender ${meldinger.size} meldinger")
            meldinger.forEach { sendAvstemmingsmelding(it) }
            oppdragDao.oppdaterAvstemteOppdrag(fagområde, avstemmingsperiode.endInclusive)
            val avstemminginfo =
                mapOf(
                    "nøkkel_fom" to avstemmingsperiode.start,
                    "nøkkel_tom" to avstemmingsperiode.endInclusive,
                    "antall_oppdrag" to oppdrag.size,
                    "antall_avstemmingsmeldinger" to meldinger.size
                )

            context.publish(avstemmingevent(id, dagen, fagområde, oppdrag.size, avstemminginfo).toJson().also {
                logger.info("sender $it")
            })
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
            utkø.send(xmlMelding)
        }
    }

    private class UtførteAvstemminger(private val oppdragDao: OppdragDao) : River.PacketListener {
        override fun onError(problems: MessageProblems, context: MessageContext) {
            logger
                .offentligError("Forstod ikke avstemming (se sikkerlogg for detaljer)")
                .privatError("Forstod ikke avstemming:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val avstemmingsnøkkelTom = packet["detaljer.nøkkel_tom"].asLong()
            val fagområde = packet["fagområde"].asText()
            logger.info("markerer ${packet["detaljer.antall_oppdrag"].asText()} oppdrag for fagområde $fagområde til og med avstemmingsnøkkel=$avstemmingsnøkkelTom som avstemte")
            oppdragDao.oppdaterAvstemteOppdrag(fagområde, avstemmingsnøkkelTom)
        }
    }
}
