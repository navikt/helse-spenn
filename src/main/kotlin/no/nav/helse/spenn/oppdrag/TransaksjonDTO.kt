package no.nav.helse.spenn.oppdrag

import java.time.LocalDateTime
import java.util.*


enum class TransaksjonStatus {
    STARTET,
    SIMULERING_OK,
    SIMULERING_FEIL,
    STOPPET,
    SENDT_OS,
    FERDIG,
    FEIL
}