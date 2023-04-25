package no.nav.helse.spenn.oppdrag

enum class Oppdragstatus {
    MOTTATT,
    OVERFØRT,
    AKSEPTERT,
    AKSEPTERT_MED_FEIL,
    AVVIST,
    FEIL;

    fun beskrivelse() = when (this) {
        MOTTATT -> "Oppdraget er mottatt, men ikke sendt til Oppdrag/UR"
        OVERFØRT -> "Oppdraget er sendt til Oppdrag/UR. Venter på kvittering"
        AKSEPTERT -> "Oppdraget ble akseptert uten feil"
        AKSEPTERT_MED_FEIL -> "Oppdraget ble akseptert, men noe er feil"
        AVVIST -> "Oppdraget ble avvist"
        FEIL -> "Spenn forstår ikke responsen fra Oppdrag"
    }
}
