package no.nav.helse.spenn.utbetaling

enum class Oppdragstatus {
    OVERFØRT,
    AKSEPTERT,
    AKSEPTERT_MED_FEIL,
    AVVIST,
    FEIL;

    fun beskrivelse() = when (this) {
        OVERFØRT -> "Oppdraget er sendt til Oppdrag/UR. Venter på kvittering"
        AKSEPTERT -> "Oppdraget ble akseptert uten feil"
        AKSEPTERT_MED_FEIL -> "Oppdraget ble akseptert, men noe er feil"
        AVVIST -> "Oppdraget ble avvist"
        FEIL -> "Spenn forstår ikke responsen fra Oppdrag"
    }
}
