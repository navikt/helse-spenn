package no.nav.helse.spenn.utbetaling

import java.time.LocalDateTime

class OppdragDto(
    private val avstemmingsnøkkel: Long,
    private val opprettet: LocalDateTime,
    private val status: Oppdragstatus
) {

    internal fun kanSendesPåNytt() = status in setOf(Oppdragstatus.MOTTATT, Oppdragstatus.AVVIST, Oppdragstatus.FEIL)
    internal fun erKvittert() = status != Oppdragstatus.MOTTATT

    internal fun somLøsning() =
        mapOf(
            "status" to status,
            "beskrivelse" to status.beskrivelse(),
            "overføringstidspunkt" to opprettet,
            "avstemmingsnøkkel" to avstemmingsnøkkel
        )
}
