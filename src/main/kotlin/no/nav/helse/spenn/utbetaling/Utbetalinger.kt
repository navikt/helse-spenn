package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.UtKø
import no.nav.helse.spenn.UtbetalingslinjerMapper
import no.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
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
        val fagsystemId = packet["Utbetaling.fagsystemId"].asText()
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
        val sjekksum = utbetalingslinjer.hashCode()

        try {
            if (oppdragDao.finnesFraFør(fødselsnummer, utbetalingId)) {
                log.warn("Motatt duplikat. Ubetalingsid $utbetalingId finnes allerede for dette fnr")
                sikkerLogg.warn("Motatt duplikat. Ubetalingsid $utbetalingId finnes allerede for $fødselsnummer")
                val gammeltOppdrag: OppdragDto = oppdragDao.hentOppdrag(fødselsnummer, utbetalingId)
                //Hvis mottatt - send gammel xml på nytt
                if (gammeltOppdrag.kanSendesPåNytt()) {
                    //Her lager vi en frankenstein av nye utbetalingslinjer og gammelt oppdrag.
                    // Dette er videreføring av gammel oppførsel, men bør nok rettes til enten å bruke gammelt eller nytt oppdrag.
                    //TODO: Sjekk at dette stemmer overens med gammel oppførsel
                    gammeltOppdrag.sendOppdrag(oppdragDao, utbetalingslinjer, nå, tilOppdrag)
                }
                packet["@løsning"] = mapOf(
                    "Utbetaling" to gammeltOppdrag.somLøsning()
                )
            } else {
                if (oppdragDao.erSjekksumDuplikat(fødselsnummer, sjekksum)) {
                    sikkerLogg.warn("Sjekksumkollisjon for fnr $fødselsnummer og utbetalingsid $utbetalingId. Indikerer duplikat utbetaling.")
                }
                val oppdragDto = oppdragDao.nyttOppdrag(
                    fagområde = packet["Utbetaling.fagområde"].asText(),
                    avstemmingsnøkkel = avstemmingsnøkkel,
                    fødselsnummer = fødselsnummer,
                    sjekksum = sjekksum,
                    organisasjonsnummer = organisasjonsnummer,
                    mottaker = mottaker,
                    tidspunkt = tidspunkt,
                    fagsystemId = fagsystemId,
                    status = Oppdragstatus.MOTTATT,
                    totalbeløp = utbetalingslinjer.totalbeløp(),
                    originalJson = packet.toJson()
                )
                oppdragDto.sendOppdrag(oppdragDao, utbetalingslinjer, nå, tilOppdrag)
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
}
