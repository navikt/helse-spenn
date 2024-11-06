package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.Avstemmingsnøkkel
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Utbetalinger::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAll("@behov", listOf("Utbetaling"))
                it.rejectKey("@løsning")
                it.requireKey("@id", "fødselsnummer", "organisasjonsnummer")
                it.interestedIn("Utbetaling.maksdato", JsonNode::asLocalDate)
                it.requireKey(
                    "Utbetaling",
                    "Utbetaling.saksbehandler",
                    "Utbetaling.mottaker",
                    "Utbetaling.fagsystemId",
                    "utbetalingId"
                )
                it.requireAny("Utbetaling.fagområde", listOf("SPREF", "SP"))
                it.requireAny("Utbetaling.endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("Utbetaling.linjer") {
                    requireKey("sats", "delytelseId", "klassekode")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    requireAny("satstype", listOf("DAG", "ENG"))
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> check(value.asText() in setOf("OPPH")) }
                    interestedIn("grad")
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Forstod ikke behov om Utbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke behov om Utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["Utbetaling.mottaker"].asText()
        val fagsystemId = packet["Utbetaling.fagsystemId"].asText().trim()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val maksdato = packet["Utbetaling.maksdato"].asOptionalLocalDate()
        if (packet["Utbetaling.linjer"].isEmpty) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        val nå = Instant.now()
        val tidspunkt = nå
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)

        try {
            val oppdragDto = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId, fagsystemId) ?: oppdragDao.nyttOppdrag(
                utbetalingId = utbetalingId,
                fagområde = packet["Utbetaling.fagområde"].asText(),
                avstemmingsnøkkel = avstemmingsnøkkel,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                mottaker = mottaker,
                tidspunkt = tidspunkt,
                fagsystemId = fagsystemId,
                status = Oppdragstatus.MOTTATT,
                totalbeløp = packet["Utbetaling.linjer"].sumOf { it.path("sats").asInt() },
                originalJson = packet.toJson()
            )

            if (oppdragDto.erKvittert()) {
                log.info("Mottatt duplikat. UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for dette fnr")
                sikkerLogg.info("Mottatt duplikat. UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for $fødselsnummer:\n${packet.toJson()}")

                //Hvis mottatt - send gammel xml på nytt
                if (!oppdragDto.kanSendesPåNytt()) {
                    packet["@løsning"] = mapOf("Utbetaling" to oppdragDto.somLøsning())
                    return context.publish(packet.toJson().also {
                        sikkerLogg.info("sender gammel løsning på utbetaling=$it")
                    })
                }

                log.info("sender eksisterende oppdrag på nytt, UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId")
                sikkerLogg.info("sender eksisterende oppdrag på nytt, UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId:\n${packet.toJson()}")

                oppdragDao.oppdaterOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, Oppdragstatus.MOTTATT)
            }

            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.MOTTATT,
                    "beskrivelse" to Oppdragstatus.MOTTATT.beskrivelse(),
                    "avstemmingsnøkkel" to avstemmingsnøkkel
                )
            )
            context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
            context.publish(lagOppdragsmelding(fødselsnummer, organisasjonsnummer, utbetalingId, fagsystemId, tidspunkt, avstemmingsnøkkel, mottaker, maksdato, packet).toJson())
        } catch (err: Exception) {
            log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
            sikkerLogg.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err, keyValue("fødselsnummer", fødselsnummer))
            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil: ${err.message}"
                )
            )
            context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        }
    }

    private fun lagOppdragsmelding(fødselsnummer: String, organisasjonsnummer: String, utbetalingId: UUID, fagsystemId: String, tidspunkt: LocalDateTime, avstemmingsnøkkel: Long, mottaker: String, maksdato: LocalDate?, packet: JsonMessage): JsonMessage {
        return JsonMessage.newMessage("oppdrag_utbetaling", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "saksbehandler" to packet["Utbetaling.saksbehandler"],
            "opprettet" to tidspunkt,
            "avstemmingsnøkkel" to avstemmingsnøkkel,
            "mottaker" to mottaker,
            "fagsystemId" to fagsystemId,
            "utbetalingId" to utbetalingId,
            "fagområde" to packet["Utbetaling.fagområde"].asText(),
            "endringskode" to packet["Utbetaling.endringskode"].asText(),
            "totalbeløp" to packet["Utbetaling.linjer"].sumOf { it.path("sats").asInt() },
            "linjer" to packet["Utbetaling.linjer"].map { linje ->
                mutableMapOf<String, Any>(
                    "fom" to linje.path("fom").asText(),
                    "tom" to linje.path("tom").asText(),
                    "endringskode" to linje.path("endringskode").asText(),
                    "sats" to linje.path("sats").asInt(),
                    "delytelseId" to linje.path("delytelseId").asInt(),
                    "satstype" to linje.path("satstype").asText(),
                    "klassekode" to linje.path("klassekode").asText()
                ).apply {
                    compute("grad") { _, _ -> linje.path("grad").takeUnless(JsonNode::isMissingOrNull)?.asInt() }
                    compute("statuskode") { _, _ -> linje.path("statuskode").takeUnless(JsonNode::isMissingOrNull)?.asText() }
                    compute("datoStatusFom") { _, _ -> linje.path("datoStatusFom").takeUnless(JsonNode::isMissingOrNull)?.asText() }
                    compute("refDelytelseId") { _, _ -> linje.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt() }
                    compute("refFagsystemId") { _, _ -> linje.path("refFagsystemId").takeUnless(JsonNode::isMissingOrNull)?.asText() }
                }
            }
        ).apply {
            compute("maksdato") { _, _ -> maksdato }
        })
    }
}
