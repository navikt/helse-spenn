package no.nav.helse.opprydding

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

internal class SlettPersonRiver(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository
): River.PacketListener {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "slett_person")
                it.requireKey("@id", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        sikkerlogg.info("Sletter person med fødselsnummer: $fødselsnummer")
        val oppdragliste = personRepository.hentSisteOppdrag(fødselsnummer)
        personRepository.slett(fødselsnummer)
        sikkerlogg.info("Annullerer oppdrag ${oppdragliste.joinToString()}")
        oppdragliste
            .map { oppdrag ->
                lagOppdragsmelding(oppdrag)
            }
            .forEach { context.publish(it.toJson()) }
    }

    private fun lagOppdragsmelding(oppdrag: PersonRepository.Oppdrag): JsonMessage {
        return JsonMessage.newMessage("oppdrag_utbetaling", mapOf(
            "fødselsnummer" to oppdrag.fnr,
            "aktørId" to oppdrag.aktørId,
            "organisasjonsnummer" to oppdrag.orgnr,
            "saksbehandler" to "SPENN",
            "opprettet" to LocalDateTime.now(),
            "avstemmingsnøkkel" to Avstemmingsnøkkel.opprett(),
            "mottaker" to oppdrag.mottaker,
            "fagsystemId" to oppdrag.fagsystemId,
            "utbetalingId" to oppdrag.utbetalingId,
            "fagområde" to oppdrag.fagomrade,
            "endringskode" to "ENDR",
            "totalbeløp" to 0,
            "linjer" to listOf(
                mapOf(
                    "fom" to oppdrag.fom,
                    "tom" to oppdrag.tom,
                    "endringskode" to "ENDR",
                    "sats" to oppdrag.sats,
                    "delytelseId" to oppdrag.delytelseId,
                    "satstype" to "DAG",
                    "klassekode" to oppdrag.klassekode,
                    "grad" to oppdrag.grad,
                    "statuskode" to "OPPH",
                    "datoStatusFom" to (oppdrag.eldsteFom ?: oppdrag.fom)
                )
            )
        ))
    }

    internal object Avstemmingsnøkkel {
        fun opprett(tidspunkt: Instant = Instant.now()) = tidspunkt.epochSecond * 10.0.pow(9).toLong() + tidspunkt.nano
    }
}