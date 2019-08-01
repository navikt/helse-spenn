package no.nav.helse.spenn

import java.time.format.DateTimeFormatter

enum class FagOmraadekode(val kode: String) {
    SYKEPENGER("SP"),
    SYKEPENGER_REFUSJON("SPREF")
}

enum class KvitteringAlvorlighetsgrad(val kode : String) {
    OK("00"),
    AKSEPTERT_MEN_NOE_ER_FEIL("04"),
    AVVIST_FUNKSJONELLE_FEIL("08"),
    AVVIST_TEKNISK_FEIL("12")
}

val avstemmingsnokkelFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
