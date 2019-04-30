package no.nav.helse.spenn.oppdrag

data class OppdragResponse(val status: OppdragStatus, val kodeMelding: String?, val alvorlighetsgrad: String,
                           val beskrMelding: String?, val fagsystemId: String)

enum class OppdragStatus {
    OK,
    AKSEPTERT_MED_FEILMELDING,
    FEIL
}