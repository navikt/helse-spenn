package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.trygdeetaten.skjema.oppdrag.TkodeStatusLinje
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class Utbetalinger(rapidsConnection: RapidsConnection, private val tilOppdrag: UtKø) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(Utbetalinger::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppdrag_utbetaling")
                it.requireKey("@id", "fødselsnummer")
                it.interestedIn("aktørId", "maksdato")
                it.requireKey("saksbehandler", "avstemmingsnøkkel", "mottaker", "fagsystemId", "utbetalingId")
                it.requireAny("fagområde", listOf("SPREF", "SP"))
                it.requireAny("endringskode", listOf("NY", "UEND", "ENDR"))
                it.requireArray("linjer") {
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
        log.error("Forstod ikke oppdrag_utbetaling (se sikkerlogg for detaljer)")
        sikkerLogg.error("Forstod ikke oppdrag_utbetaling:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("løser utbetalingsbehov id=${packet["@id"].asText()}")
        val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val mottaker = packet["mottaker"].asText()
        val fagsystemId = packet["fagsystemId"].asText().trim()

        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())

        val endringskode = packet["endringskode"].asText()
        val saksbehandler = packet["saksbehandler"].asText()
        val maksdato = packet["maksdato"].asOptionalLocalDate()

        withMDC(mapOf(
            "utbetalingId" to utbetalingId.toString(),
            "fagsystemId" to fagsystemId,
            "aktørId" to packet["aktørId"].asText("ikke oppgitt")
        )) {
            val fagområde = packet["fagområde"].asText()
            val utbetalingslinjer = when (fagområde) {
                "SPREF" -> refusjonTilArbeidsgiver(mottaker, fagsystemId, fødselsnummer, endringskode, saksbehandler, maksdato)
                "SP" -> utbetalingTilBruker(mottaker, fagsystemId, fødselsnummer, endringskode, saksbehandler, maksdato)
                else -> throw IllegalArgumentException("ukjent fagområde $fagområde")
            }.apply {
                packet["linjer"].forEach { linje ->
                    Utbetalingslinjer.Utbetalingslinje(
                        delytelseId = linje["delytelseId"].asInt(),
                        endringskode = linje["endringskode"].asText(),
                        klassekode = linje["klassekode"].asText(),
                        fom = linje["fom"].asLocalDate(),
                        tom = linje["tom"].asLocalDate(),
                        sats = linje["sats"].asInt(),
                        satstype = linje["satstype"].asText(),
                        grad = linje["grad"].takeUnless(JsonNode::isMissingOrNull)?.asInt(),
                        refDelytelseId = linje.path("refDelytelseId").takeUnless(JsonNode::isMissingOrNull)?.asInt(),
                        refFagsystemId = linje.path("refFagsystemId").takeUnless(JsonNode::isMissingOrNull)?.asText()?.trim(),
                        datoStatusFom = linje.path("datoStatusFom").takeUnless(JsonNode::isMissingOrNull)?.asLocalDate(),
                        statuskode = linje.path("statuskode").takeUnless(JsonNode::isMissingOrNull)?.asText()
                    ).also { linje(it) }
                }
            }

            val oppdrag = OppdragBuilder(utbetalingId, utbetalingslinjer, avstemmingsnøkkel).build()
            val oppdragXml = OppdragXml.marshal(oppdrag)
            try {
                tilOppdrag.send(oppdragXml)
                packet["kvittering"] = mapOf(
                    "status" to Oppdragstatus.OVERFØRT,
                    "beskrivelse" to "Meldingen er videresendt på MQ til Oppdrag",
                    "xmlmelding" to oppdragXml
                )
            } catch (err: Exception) {
                log.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
                sikkerLogg.error("Teknisk feil ved utbetaling for behov id=${packet["@id"].asText()}: ${err.message}", err)
                packet["kvittering"] = mapOf(
                    "status" to Oppdragstatus.FEIL,
                    "beskrivelse" to "Kunne ikke opprette nytt Oppdrag pga. teknisk feil",
                    "feilmelding" to err.message
                )

                // kast exception videre oppover; dersom MQ er nede ønsker vi at appen skal restarte
                if (err is MQErNede) throw err
            } finally {
                context.publish(packet.toJson().also { sikkerLogg.info("sender løsning på oppdrag_utbetaling=$it") })
            }
        }
    }


    private fun refusjonTilArbeidsgiver(mottaker: String, fagsystemId: String, fødselsnummer: String, endringskode: String, saksbehandler: String, maksdato: LocalDate?) =
        Utbetalingslinjer.RefusjonTilArbeidsgiver(
            mottaker = mottaker,
            fagsystemId = fagsystemId,
            fødselsnummer = fødselsnummer,
            endringskode = endringskode,
            saksbehandler = saksbehandler,
            maksdato = maksdato
        )

    private fun utbetalingTilBruker(mottaker: String, fagsystemId: String, fødselsnummer: String, endringskode: String, saksbehandler: String, maksdato: LocalDate?) =
        Utbetalingslinjer.UtbetalingTilBruker(
            fagsystemId = fagsystemId,
            fødselsnummer = fødselsnummer,
            mottaker = mottaker,
            endringskode = endringskode,
            saksbehandler = saksbehandler,
            maksdato = maksdato
        )
}
