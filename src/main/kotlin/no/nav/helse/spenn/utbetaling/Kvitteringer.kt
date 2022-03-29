package no.nav.helse.spenn.utbetaling

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.Kø
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Kvitteringer(
    private val rapidsConnection: RapidsConnection,
    fraOppdrag: Kø,
    private val oppdragDao: OppdragDao
) {
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
        val feilkode = requireNotNull(oppdrag.mmel.alvorlighetsgrad)
        val meldingFraOppdrag = oppdrag.mmel.beskrMelding
        val (status, beskrivelse) = when (feilkode) {
            "00" -> Oppdragstatus.AKSEPTERT to (meldingFraOppdrag ?: "Oppdraget ble akseptert uten feil")
            "04" -> Oppdragstatus.AKSEPTERT_MED_FEIL to (meldingFraOppdrag
                ?: "Oppdraget ble akseptert, men noe er feil")
            "08" -> Oppdragstatus.AVVIST to (meldingFraOppdrag ?: "Oppdraget ble avvist")
            "12" -> Oppdragstatus.FEIL to "Teknisk feil fra oppdrag, forsøk utbetaling på nytt"
            else -> Oppdragstatus.FEIL to "Spenn forstår ikke responsen fra Oppdrag. Fikk ukjent kode: $feilkode"
        }

        check(oppdragDao.oppdaterOppdrag(avstemmingsnøkkel, fagsystemId, status, beskrivelse, feilkode, xmlMessage)) {
            "Klarte ikke å oppdatere oppdrag i databasen!"
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
            "avstemmingsnøkkel" to avstemmingsnøkkel,
            "fagsystemId" to fagsystemId,
            "status" to status,
            "feilkode_oppdrag" to feilkode,
            "beskrivelse" to beskrivelse,
            "originalXml" to xmlMessage
        ).apply {
            compute("@forårsaket_av") { _, _ ->
                oppdragDao.hentBehovForOppdrag(avstemmingsnøkkel)?.let {
                    mapOf("id" to it.id)
                }
            }
        }).toJson().also { sikkerLogg.info("sender transaksjon status=$it") })
    }
}
