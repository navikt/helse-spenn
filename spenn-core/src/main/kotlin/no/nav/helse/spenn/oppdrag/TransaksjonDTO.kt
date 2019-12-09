package no.nav.helse.spenn.oppdrag


enum class TransaksjonStatus {
    STARTET,
    SIMULERING_OK,
    SIMULERING_FEIL,
    STOPPET,
    SENDT_OS,
    FERDIG,
    FEIL
}