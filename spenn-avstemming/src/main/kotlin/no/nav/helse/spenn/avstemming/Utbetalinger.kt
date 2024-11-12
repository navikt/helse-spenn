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
import java.util.*

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val logger = Logg.ny(Utbetalinger::class)
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppdrag_utbetaling")
                it.rejectKey("kvittering")
                it.requireKey("@id", "utbetalingId", "fagsystemId")
                it.requireKey("fødselsnummer", "mottaker", "avstemmingsnøkkel", "fagområde", "opprettet", "totalbeløp")
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        logger
            .offentligError("Forstod ikke oppdrag_utbetaling (se sikkerlogg for detaljer)")
            .privatError("Forstod ikke oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val fagsystemId = packet["fagsystemId"].asText()
        val fagområde = packet["fagområde"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val mottaker = packet["mottaker"].asText()
        val totalbeløp = packet["totalbeløp"].asInt()
        val opprettet = packet["opprettet"].asLocalDateTime()

        val pakkelogg = logger
            .åpent("meldingsreferanseId", packet["@id"].asText())
            .åpent("utbetalingId", "$utbetalingId")
            .åpent("fagsystemId", fagsystemId)
            .privat("fødselsnummer", fødselsnummer)
        if (oppdragDao.nyttOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet)) {
            pakkelogg.info("opprettet oppdrag med avstemmingsnøkkel $avstemmingsnøkkel")
        } else {
            pakkelogg.error("kan ikke opprette oppdrag, finnes avstemmingsnøkkel $avstemmingsnøkkel fra før?")
        }
    }
}
