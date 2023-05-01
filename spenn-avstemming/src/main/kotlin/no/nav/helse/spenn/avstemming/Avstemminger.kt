package no.nav.helse.spenn.avstemming

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Avstemminger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao,
    private val avstemmingDao: AvstemmingDao,
    private val utkø: UtKø,
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
        }.register(UtførAvstemming())
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "hel_time")
                it.requireValue("time", "9")
                it.require("dagen", JsonNode::asLocalDate)
            }
        }.register(AvstemmingBegivenhet())
    }

    // utfører avstemming fra spisset signal, f.eks. fra spout
    private inner class UtførAvstemming : River.PacketListener {
        override fun onError(problems: MessageProblems, context: MessageContext) {
            logger
                .offentligError("Forstod ikke utfør_avstemming (se sikkerlogg for detaljer)")
                .privatError("Forstod ikke utfør_avstemming:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val dagen = packet["dagen"].asLocalDate()
            avstem(dagen, context)
        }
    }

    private inner class AvstemmingBegivenhet : River.PacketListener {
        override fun onError(problems: MessageProblems, context: MessageContext) {
            logger
                .offentligError("Forstod ikke hel_time (se sikkerlogg for detaljer)")
                .privatError("Forstod ikke hel_time:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            // avstemmer gårsdagen
            val dagen = packet["dagen"].asLocalDate().minusDays(1)
            avstem(dagen, context)
        }
    }

    private fun avstem(dagen: LocalDate, context: MessageContext) {
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
