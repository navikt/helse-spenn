package no.nav.helse.spenn.oppdrag

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Kvitteringer(private val rapidsConnection: RapidsConnection, fraOppdrag: Kø) {
    private companion object {
        private val log = LoggerFactory.getLogger(Kvitteringer::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
    init {
        fraOppdrag.setMessageListener { message ->
            try {
                val body = OppdragXml.normalizeXml(message)
                try {
                    sikkerLogg.info("mottok kvittering fra oppdrag body:\n$body")
                    onMessage(body)
                } catch (err: Exception) {
                    log.error("Feil med mottak av MQ-melding: ${err.message}", err)
                    sikkerLogg.error("Feil med mottak av MQ-melding: ${err.message}\n$body", err)
                }
            } catch (err: Exception) {
                log.error("Klarte ikke å hente ut meldingsinnholdet: ${err.message}", err)
            }
        }
    }

    private fun onMessage(xmlMessage: String) {
        rapidsConnection.publish(JsonMessage.newMessage(
            mapOf(
                "@event_name" to "oppdrag_kvittering",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "originalXml" to xmlMessage
            )
        ).toJson().also { sikkerLogg.info("sender oppdrag_kvittering:\n$it") })

        val oppdrag = OppdragXml.unmarshal(xmlMessage)
        val avstemmingsnøkkel = requireNotNull(oppdrag.oppdrag110.avstemming115.nokkelAvstemming).toLong()
        val fagsystemId = requireNotNull(oppdrag.oppdrag110.fagsystemId)
        val fødselsnummer = requireNotNull(oppdrag.oppdrag110.oppdragGjelderId)
        val utbetalingId = UUID.fromString(requireNotNull(oppdrag.oppdrag110.oppdragsLinje150.first().henvisning))
        val feilkode = requireNotNull(oppdrag.mmel.alvorlighetsgrad)
        val meldingFraOppdrag: String? = oppdrag.mmel.beskrMelding
        val kodemelding: String? = oppdrag.mmel.kodeMelding
        /*
            08 er feil i valideringen, f.eks. at vi sender med feil verdi i klassekode, grad m.m. og det må rettes opp hos før oppdraget kan sendes på nytt. Skal avstemmes som AVVIST
            12 er en teknisk feil som f.eks. at OS ikke får kontakt med PDL, ereg o.l. og kan forsøke å resendes fra oss. Skal avstemmes som AVVIST.

            i praksis vil 08 bety at oppdraget som sendes fra spleis er feil, og må forkastes/lages på nytt, derfor tolket vi AVVIST (08) som "a hard no"
         */
        val (status, beskrivelse) = when (feilkode) {
            "00" -> Oppdragstatus.AKSEPTERT to (meldingFraOppdrag ?: "Oppdraget ble akseptert uten feil")
            "04" -> Oppdragstatus.AKSEPTERT_MED_FEIL to (meldingFraOppdrag
                ?: "Oppdraget ble akseptert, men noe er feil")
            "08" -> Oppdragstatus.AVVIST to (meldingFraOppdrag ?: "Oppdraget ble avvist")
            "12" -> Oppdragstatus.FEIL to "Teknisk feil fra oppdrag, OS har angivelig forsøkt et par ganger og til slutt avvist. Forsøk på nytt"
            else -> Oppdragstatus.FEIL to "Spenn forstår ikke responsen fra Oppdrag. Fikk ukjent kode: $feilkode"
        }

        sikkerLogg.info(
            "fødselsnummer=$fødselsnummer avstemmingsnøkkel=$avstemmingsnøkkel fagsystemId=$fagsystemId " +
                    "feilkode=$feilkode status=$status beskrivelse=$beskrivelse"
        )

        rapidsConnection.publish(fødselsnummer, JsonMessage.newMessage(mutableMapOf<String, Any>(
            "@event_name" to "transaksjon_status",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "fødselsnummer" to fødselsnummer,
            "utbetalingId" to utbetalingId,
            "avstemmingsnøkkel" to avstemmingsnøkkel,
            "fagsystemId" to fagsystemId,
            "status" to status,
            "feilkode_oppdrag" to feilkode,
            "beskrivelse" to beskrivelse,
            "originalXml" to xmlMessage
        ).apply {
            compute("kodemelding") { _, _ -> kodemelding }
            compute("beskrivendemelding") { _, _ -> meldingFraOppdrag }
        }).toJson().also { sikkerLogg.info("sender transaksjon status=$it") })
    }
}
