package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.spenn.Metrics.tellDuplikatVedtak
import no.nav.helse.spenn.oppdrag.Utbetalingsbehov
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.oppdrag.dao.SanityCheckException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDate

class UtbetalingLøser(
    rapidsConnection: RapidsConnection,
    private val oppdragService: OppdragService
) : River.PacketListener {

    private val log = LoggerFactory.getLogger("UtbetalingLøser")

    init {
        River(rapidsConnection).apply {
            validate { it.requireAll("@behov", listOf("Utbetaling")) }
            validate { it.forbid("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("utbetalingsreferanse") }
            validate { it.requireKey("utbetalingslinjer") }
            validate { it.requireKey("maksdato") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("saksbehandler") }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {}

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        saveInitialOppdragState(lagOppdragFraBehov(packet))
    }

    private fun saveInitialOppdragState(utbetaling: Utbetalingsbehov) {
        try {
            oppdragService.lagreNyttOppdrag(utbetaling)
        } catch (e: SQLIntegrityConstraintViolationException) {
            log.error("skipping duplicate for key ${utbetaling.utbetalingsreferanse}")
            tellDuplikatVedtak()
        } catch (e: PSQLException) { // TODO : Burde oppdragservice fange disse to og gi spenn duplikat exc?
            if (e.sqlState == "23505") {
                log.error("skipping duplicate for key ${utbetaling.utbetalingsreferanse}")
                tellDuplikatVedtak()
            } else {
                throw e
            }
        } catch (e: SanityCheckException) {
            log.warn(e.message)
        }
    }

    companion object {
        internal fun lagOppdragFraBehov(jsonMessage: JsonMessage) = Utbetalingsbehov(
            behov = jsonMessage.toJson(),
            utbetalingsreferanse = jsonMessage["utbetalingsreferanse"].asText(),
            oppdragGjelder = jsonMessage["fødselsnummer"].asText(),
            saksbehandler = jsonMessage["saksbehandler"].asText(),
            utbetaling = Utbetalingsbehov.Utbetaling(
                maksdato = jsonMessage["maksdato"].asText().let { LocalDate.parse(it) },
                organisasjonsnummer = jsonMessage["organisasjonsnummer"].asText(),
                utbetalingsLinjer = jsonMessage["utbetalingslinjer"].map { linje ->
                    Utbetalingsbehov.Linje(
                        utbetalesTil = jsonMessage["organisasjonsnummer"].asText(),
                        sats = BigDecimal(linje["dagsats"].asText()),
                        datoFom = linje["fom"].asText().let { LocalDate.parse(it) },
                        datoTom = linje["tom"].asText().let { LocalDate.parse(it) }
                    )
                }
            )
        )

        internal fun lagAnnulleringsoppdragFraBehov(jsonMessage: JsonMessage) = Utbetalingsbehov(
            behov = jsonMessage.toJson(),
            utbetalingsreferanse = jsonMessage["utbetalingsreferanse"].asText(),
            oppdragGjelder = jsonMessage["fødselsnummer"].asText(),
            saksbehandler = jsonMessage["saksbehandler"].asText(),
            utbetaling = null
        )
    }
}

internal fun JsonNode.toAnulleringsbehov() = JsonMessage(
    this.toString(),
    MessageProblems("")
).also {
    it.requireKey("fødselsnummer")
    it.requireKey("saksbehandler")
    it.requireKey("utbetalingsreferanse")
    it.interestedIn("utbetalingslinjer")
}

internal fun JsonNode.toOppdragsbehov() = JsonMessage(
    this.toString(),
    MessageProblems("")
).also {
    it.requireKey("fødselsnummer")
    it.requireKey("utbetalingsreferanse")
    it.requireKey("utbetalingslinjer")
    it.requireKey("maksdato")
    it.requireKey("organisasjonsnummer")
    it.requireKey("saksbehandler")
}
