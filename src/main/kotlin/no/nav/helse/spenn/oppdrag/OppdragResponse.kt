package no.nav.helse.spenn.oppdrag

data class Kvittering(val status: KvitteringStatus, val kodeMelding: String?, val alvorlighetsgrad: String,
                      val beskrMelding: String?, val fagsystemId: String)

enum class KvitteringStatus {
    OK,
    AKSEPTERT_MED_FEILMELDING,
    FEIL
}