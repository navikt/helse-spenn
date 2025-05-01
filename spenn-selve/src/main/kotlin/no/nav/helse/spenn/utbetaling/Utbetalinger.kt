package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spenn.Avstemmingsnøkkel
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.sumOf

private val log = LoggerFactory.getLogger(Utbetalinger::class.java)
private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    private val oppdragDao: OppdragDao
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("Utbetaling"))
                it.forbid("@løsning")
            }
            validate {
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
                    requireKey("sats", "delytelseId", "klassekode", "grad")
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                    requireValue("satstype", "DAG")
                    interestedIn("datoStatusFom", JsonNode::asLocalDate)
                    interestedIn("statuskode") { value -> check(value.asText() in setOf("OPPH")) }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Forstod ikke behov om Utbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke behov om Utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val mottaker = packet["Utbetaling.mottaker"].asText()
        val fagområde = packet["Utbetaling.fagområde"].asText()
        val endringskode = packet["Utbetaling.endringskode"].asText()
        val fagsystemId = packet["Utbetaling.fagsystemId"].asText().trim()
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val maksdato = packet["Utbetaling.maksdato"].asOptionalLocalDate()
        val saksbehandler = packet["Utbetaling.saksbehandler"].asText()

        håndterUtbetalingsbehov(
            behovnavn = "Utbetaling",
            context = context,
            oppdragDao = oppdragDao,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            endringskode = endringskode,
            utbetalingId = utbetalingId,
            saksbehandler = saksbehandler,
            maksdato = maksdato,
            linjer = packet["Utbetaling.linjer"],
            packet = packet
        )
    }
}

internal fun håndterUtbetalingsbehov(
    behovnavn: String,
    context: MessageContext,
    oppdragDao: OppdragDao,
    fødselsnummer: String,
    organisasjonsnummer: String,
    mottaker: String,
    fagområde: String,
    endringskode: String,
    fagsystemId: String,
    utbetalingId: UUID,
    saksbehandler: String,
    maksdato: LocalDate?,
    linjer: JsonNode,
    packet: JsonMessage
) {
    if (linjer.isEmpty) return log.info("ingen utbetalingslinjer id=${packet["@id"].asText()}; ignorerer behov")
    val nå = Instant.now()
    val tidspunkt = nå
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)

    try {
        val oppdragDto = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId, fagsystemId) ?: oppdragDao.nyttOppdrag(
            utbetalingId = utbetalingId,
            fagområde = fagområde,
            avstemmingsnøkkel = avstemmingsnøkkel,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            mottaker = mottaker,
            tidspunkt = tidspunkt,
            fagsystemId = fagsystemId,
            status = Oppdragstatus.MOTTATT,
            totalbeløp = linjer.sumOf { it.path("sats").asInt() },
            originalJson = packet.toJson()
        )

        if (oppdragDto.erKvittert()) {
            log.info("Mottatt duplikat. UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for dette fnr")
            sikkerLogg.info("Mottatt duplikat. UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId finnes allerede for $fødselsnummer:\n${packet.toJson()}")

            //Hvis mottatt - send gammel xml på nytt
            if (!oppdragDto.kanSendesPåNytt()) {
                packet["@løsning"] = mapOf(behovnavn to oppdragDto.somLøsning())
                return context.publish(packet.toJson().also {
                    sikkerLogg.info("sender gammel løsning på utbetaling=$it")
                })
            }

            log.info("sender eksisterende oppdrag på nytt, UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId")
            sikkerLogg.info("sender eksisterende oppdrag på nytt, UtbetalingId=$utbetalingId, fagsystemId=$fagsystemId:\n${packet.toJson()}")

            oppdragDao.oppdaterOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, Oppdragstatus.MOTTATT)
        }

        packet["@løsning"] = mapOf(
            behovnavn to mapOf(
                "status" to Oppdragstatus.MOTTATT,
                "beskrivelse" to Oppdragstatus.MOTTATT.beskrivelse(),
                "avstemmingsnøkkel" to avstemmingsnøkkel
            )
        )
        context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
        context.publish(lagOppdragsmelding(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            endringskode = endringskode,
            tidspunkt = tidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            mottaker = mottaker,
            maksdato = maksdato,
            saksbehandler = saksbehandler,
            linjer = linjer
        ).toJson())
    } catch (err: Exception) {
        log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
        sikkerLogg.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err, keyValue("fødselsnummer", fødselsnummer))
        packet["@løsning"] = mapOf(
            behovnavn to mapOf(
                "status" to Oppdragstatus.FEIL,
                "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil: ${err.message}"
            )
        )
        context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på utbetaling=$it") })
    }
}

private fun lagOppdragsmelding(
    fødselsnummer: String,
    organisasjonsnummer: String,
    utbetalingId: UUID,
    fagområde: String,
    fagsystemId: String,
    endringskode: String,
    tidspunkt: LocalDateTime,
    avstemmingsnøkkel: Long,
    mottaker: String,
    saksbehandler: String,
    maksdato: LocalDate?,
    linjer: JsonNode
): JsonMessage {
    return JsonMessage.newMessage("oppdrag_utbetaling", mutableMapOf(
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "saksbehandler" to saksbehandler,
        "opprettet" to tidspunkt,
        "avstemmingsnøkkel" to avstemmingsnøkkel,
        "mottaker" to mottaker,
        "fagsystemId" to fagsystemId,
        "utbetalingId" to utbetalingId,
        "fagområde" to fagområde,
        "endringskode" to endringskode,
        "totalbeløp" to linjer.sumOf { it.path("sats").asInt() },
        "linjer" to linjer.map { linje ->
            mutableMapOf<String, Any>(
                "fom" to linje.path("fom").asText(),
                "tom" to linje.path("tom").asText(),
                "endringskode" to linje.path("endringskode").asText(),
                "sats" to linje.path("sats").asInt(),
                "delytelseId" to linje.path("delytelseId").asInt(),
                "satstype" to linje.path("satstype").asText(),
                "klassekode" to linje.path("klassekode").asText()
            ).apply {
                // grad settes ikke på feriepengeutbetalinger
                compute("grad") { _, _ -> linje.path("grad").takeUnless(JsonNode::isMissingOrNull)?.asInt() }
                compute("statuskode") { _, _ -> linje.path("statuskode").takeUnless(JsonNode::isMissingOrNull)?.asText() }
                compute("datoStatusFom") { _, _ -> linje.path("datoStatusFom").takeUnless(JsonNode::isMissingOrNull)?.asText() }
                compute("refDelytelseId") { _, _ -> linje.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt() }
                compute("refFagsystemId") { _, _ -> linje.path("refFagsystemId").takeUnless(JsonNode::isMissingOrNull)?.asText() }
            }
        }
    ).apply {
        // maksdato settes ikke på feriepengeutbetalinger eller annulleringer
        compute("maksdato") { _, _ -> maksdato }
    })
}
