package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.*

internal class Overføringer(rapidsConnection: RapidsConnection, private val oppdragDao: OppdragDao) : River.PacketListener {
    private companion object {
        private val logger = Logg.ny(Overføringer::class)
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppdrag_utbetaling")
                it.demandKey("kvittering")
                it.requireKey("@id", "aktørId", "fødselsnummer", "utbetalingId", "fagsystemId")
                it.requireKey("avstemmingsnøkkel")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("kvittering.status", listOf("OVERFØRT", "FEIL"))
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger
            .offentligError("Forstod ikke kvittering på oppdrag_utbetaling (se sikkerlogg for detaljer)")
            .privatError("Forstod ikke kvittering på oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val pakkelogg = logger
            .åpent("meldingsreferanseId", packet["@id"].asText())
            .åpent("aktørId", packet["aktørId"].asText())
            .åpent("utbetalingId", packet["utbetalingId"].asText())
            .åpent("fagsystemId", packet["fagsystemId"].asText())
            .privat("fødselsnummer", packet["fødselsnummer"].asText())

        if (packet["kvittering.status"].asText() != "OVERFØRT") {
            pakkelogg.info("ignorerer kvittering pga. status er ${packet["kvittering.status"].asText()}")
            return
        }
        if (oppdragDao.oppdragOverført(packet["avstemmingsnøkkel"].asLong())) {
            pakkelogg.info("oppdrag merket som overført til OS")
        } else {
            pakkelogg.info("oppdraget ble ikke endret, er det mottatt kvittering allerede?")
        }
    }

}
