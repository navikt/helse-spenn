package no.nav.helse.spenn


class OppdragSkjemaConstants {

    companion object {
        const val SP_ENHET = "8020"
        const val BOS = "BOS"
        const val APP = "SPENN"
    }

}

enum class KlassifiseringsKode(val kode: String) {
    SPREFAG_IOP("SPREFAG-IOP")
}

enum class AksjonsKode(val kode: String) {
    OPPDATER("1"),
    SIMULERING("S")
}

enum class EndringsKode(val kode : String) {
    NY("NY"),
    UENDRET("UEND"),
    ENDRING("ENDR")
}

enum class UtbetalingsfrekvensKode(val kode : String ) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MÅNEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSUTBETALING("ENG")
}

enum class KomponentKode(val kode: String) {
    SYKEPENGER("SP")
}

enum class BilagsTypeKode(val kode: String) {
    ORDINÆR("ORDI"),
    MEMORIAL("MEMO"),
    NØDUTBETALING("NODU")
}

enum class SatsTypeKode(val kode : String ) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MÅNEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSBELØP("ENG"),
    ÅRLIG("AAR"),
    A_KONTO("AKTO"),
    UKJENT("-");

    companion object {
        fun fromKode(kode: String): SatsTypeKode {
            for (s in values()) {
                if (s.kode == kode ) return s
            }
            return UKJENT
        }
    }
}

enum class GradTypeKode(val kode: String ) {
    UFØREGRAD("UFOR"),
    UTBETALINGSGRAD("UBGR"),
    UTTAKSGRAD_ALDERSPENSJON("UTAP"),
    UTTAKSGRAD_AFP("AFPG")

}

enum class UtbetalingsType(val kode: String) {
    YTELSE("YTEL"),
    FEILUTBETALING("FEIL"),
    FORSKUDSSKATT("SKAT"),
    JUSTERING("JUST"),
    TREKK("TREK"),
    UDEFINERT("-");

    companion object {
        fun fromKode(kode: String): UtbetalingsType {
            for (u in values()) {
                if (u.kode == kode ) return u
            }
            return UDEFINERT
        }
    }
}
