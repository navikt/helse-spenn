package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtKø
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.nav.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.util.*

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao,
    private val tilOppdrag: UtKø
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
                it.interestedIn("aktørId")
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
                    interestedIn("statuskode") { value -> TkodeStatusLinje.valueOf(value.asText()) }
                    interestedIn("grad")
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Fikk et Utbetaling-behov vi ikke validerte:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["Utbetaling.mottaker"].asText()
        val fagsystemId = packet["Utbetaling.fagsystemId"].asText().trim()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val utbetalingslinjer =
            UtbetalingslinjerMapper(packet["fødselsnummer"].asText(), packet["organisasjonsnummer"].asText())
                .fraBehov(packet["Utbetaling"])
        if (utbetalingslinjer.isEmpty()) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
        val nå = Instant.now()
        val tidspunkt = nå
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)

        try {
            if (oppdragDao.finnesFraFør(fødselsnummer, utbetalingId, fagsystemId)) {
                log.info("Motatt duplikat. UbetalingsId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for dette fnr")
                sikkerLogg.info("Motatt duplikat. UbetalingsId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for $fødselsnummer")
                val gammeltOppdrag: OppdragDto = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId, fagsystemId)
                //Hvis mottatt - send gammel xml på nytt
                if (gammeltOppdrag.kanSendesPåNytt()) {
                    if (System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp") {
                        sendOppdrag(context, fødselsnummer, organisasjonsnummer, utbetalingId, fagsystemId, avstemmingsnøkkel, mottaker, packet)
                    } else {
                        //Her lager vi en blander vi nye utbetalingslinjer og gammelt oppdrag.
                        // Behov-json i basen kan være på et gammelt format og da kan vi ikke parse.
                        // Det er opp til avsender å alltid sende samme oppdrag for samme utbetalingsid
                        gammeltOppdrag.sendOppdrag(oppdragDao, utbetalingslinjer, nå, tilOppdrag)
                    }
                }
                packet["@løsning"] = mapOf(
                    "Utbetaling" to gammeltOppdrag.somLøsning()
                )
            } else {
                val oppdragDto = oppdragDao.nyttOppdrag(
                    utbetalingId = utbetalingId,
                    fagområde = packet["Utbetaling.fagområde"].asText(),
                    avstemmingsnøkkel = avstemmingsnøkkel,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    mottaker = mottaker,
                    tidspunkt = tidspunkt,
                    fagsystemId = fagsystemId,
                    status = Oppdragstatus.MOTTATT,
                    totalbeløp = utbetalingslinjer.totalbeløp(),
                    originalJson = packet.toJson()
                )
                if (System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp") {
                    sendOppdrag(context, fødselsnummer, organisasjonsnummer, utbetalingId, fagsystemId, avstemmingsnøkkel, mottaker, packet)
                } else {
                    oppdragDto.sendOppdrag(oppdragDao, utbetalingslinjer, nå, tilOppdrag)
                }
                packet["@løsning"] = mapOf(
                    "Utbetaling" to oppdragDto.somLøsning()
                )
            }
        } catch (err: Exception) {
            log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
            packet["@løsning"] = mapOf(
                "Utbetaling" to mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil"
                )
            )

            // kast exception videre oppover; dersom MQ er nede ønsker vi at spenn skal restarte
            if (err is MQErNede) throw err
        } finally {
            context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        }
    }

    private fun sendOppdrag(context: MessageContext, fødselsnummer: String, organisasjonsnummer: String, utbetalingId: UUID, fagsystemId: String, avstemmingsnøkkel: Long, mottaker: String, packet: JsonMessage) {
        context.publish(JsonMessage.newMessage("oppdrag_utbetaling", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "saksbehandler" to packet["Utbetaling.saksbehandler"],
            "avstemmingsnøkkel" to avstemmingsnøkkel,
            "mottaker" to mottaker,
            "fagsystemId" to fagsystemId,
            "utbetalingId" to utbetalingId,
            "fagområde" to packet["Utbetaling.fagområde"].asText(),
            "endringskode" to packet["Utbetaling.endringskode"].asText(),
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
            compute("aktørId") { _, _ -> packet["aktørId"].asText() }
        }).toJson())
    }
}
