package no.nav.helse.spenn.avstemming

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

internal class Transaksjoner(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val logger = Logg.ny(Transaksjoner::class)
    }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "transaksjon_status") }
            validate {
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "fødselsnummer", "fagsystemId", "utbetalingId")
                it.requireKey("avstemmingsnøkkel", "feilkode_oppdrag", "originalXml")
                it.requireAny("status", listOf("AKSEPTERT", "AKSEPTERT_MED_FEIL", "AVVIST", "FEIL"))
                it.interestedIn("kodemelding", "beskrivendemelding")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        logger
            .offentligError("Forstod ikke transaksjon_status (se sikkerlogg for detaljer)")
            .privatError("Forstod ikke transaksjon_status:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val pakkelogg = logger
            .åpent("meldingsreferanseId", packet["@id"].asText())
            .åpent("utbetalingId", packet["utbetalingId"].asText())
            .åpent("fagsystemId", packet["fagsystemId"].asText())
            .privat("fødselsnummer", packet["fødselsnummer"].asText())

        val alvorlighetsgrad = packet["feilkode_oppdrag"].asText()
        val oppdragstatus = when (alvorlighetsgrad) {
            "00" -> Oppdragstatus.AKSEPTERT
            "04" -> Oppdragstatus.AKSEPTERT_MED_VARSEL
            "08", "12" -> Oppdragstatus.AVVIST
            else -> error("ukjent alvorlighetsgrad: $alvorlighetsgrad")
        }

        if (oppdragDao.medKvittering(
                avstemmingsnøkkel,
                oppdragstatus,
                alvorlighetsgrad,
                packet["kodemelding"].takeIf(JsonNode::isTextual)?.asText(),
                packet["beskrivendemelding"].takeIf(JsonNode::isTextual)?.asText(),
                packet["originalXml"].asText()
            )) {
            pakkelogg.info("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel er oppdatert med status=$oppdragstatus")
        } else {
            pakkelogg
                .offentligError("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel finnes ikke")
                .privatError("oppdrag med avstemmingsnøkkel $avstemmingsnøkkel for fnr=${packet["fødselsnummer"].asText()} finnes ikke")
        }

    }
}
