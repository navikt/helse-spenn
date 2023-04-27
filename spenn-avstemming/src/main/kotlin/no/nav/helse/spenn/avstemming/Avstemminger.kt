package no.nav.helse.spenn.avstemming

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

internal class Avstemminger(rapidsConnection: RapidsConnection, private val oppdragDao: OppdragDao) : River.PacketListener {
    private companion object {
        private val logger = Logg.ny(Avstemminger::class)
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "avstemming")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireKey("detaljer.nøkkel_tom")
                it.requireKey("detaljer.antall_oppdrag")
            }
        }.register(this)
    }

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
