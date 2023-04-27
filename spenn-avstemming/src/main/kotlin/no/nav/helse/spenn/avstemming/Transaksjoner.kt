package no.nav.helse.spenn.avstemming

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

internal class Transaksjoner(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val logger = Logg.ny(Transaksjoner::class)
    }

    private enum class Alvorlighetskode { AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL };

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "transaksjon_status")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "fødselsnummer", "fagsystemId")
                it.requireKey("avstemmingsnøkkel", "feilkode_oppdrag", "originalXml")
                it.requireAny("status", listOf("AKSEPTERT", "AKSEPTERT_MED_FEIL", "AVVIST", "FEIL"))
                it.interestedIn("kodemelding", "beskrivendemelding")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger
            .offentligError("Forstod ikke transaksjon_status (se sikkerlogg for detaljer)")
            .privatError("Forstod ikke transaksjon_status:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val alvorlighetskode = Alvorlighetskode.valueOf(packet["status"].asText())

        val pakkelogg = logger.fellesKontekst(mapOf(
            "meldingsreferanseId" to packet["@id"].asText(),
            "fagsystemId" to packet["fagsystemId"].asText()
        ))

        val oppdragstatus = when (alvorlighetskode) {
            Alvorlighetskode.FEIL, Alvorlighetskode.AVVIST -> Oppdragstatus.AVVIST
            Alvorlighetskode.AKSEPTERT_MED_FEIL -> Oppdragstatus.AKSEPTERT_MED_VARSEL
            else -> Oppdragstatus.AKSEPTERT
        }

        if (oppdragDao.medKvittering(
                avstemmingsnøkkel,
                oppdragstatus,
                packet["feilkode_oppdrag"].asText(),
                packet["kodemelding"].takeIf(JsonNode::isTextual)?.asText(),
                packet["beskrivendemelding"].takeIf(JsonNode::isTextual)?.asText(),
                packet["originalXml"].asText()
            )) {
            pakkelogg.info("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel er oppdatert med status=$oppdragstatus")
        } else {
            pakkelogg
                .offentligError("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel finnes ikke, eller er allerede avstemt?")
                .privatError("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel for fnr=${packet["fødselsnummer"].asText()} finnes ikke, eller er allerede avstemt?")
        }

    }
}
