package no.nav.helse.integrasjon.okonomi.oppdrag

import no.trygdeetaten.skjema.oppdrag.Oppdrag

class OppdragSkjemaConstants {

    companion object {
        @JvmField val JAXB_CLASS = Oppdrag::class.java
    }
}

enum class AksjonsKode(val kode: String) {
    OPPDATER("1"),
    SIMULERING("3")
}

enum class EndringsKode(val kode : String) {
    NY("NY"),
    UENDRET("UEND"),
    ENDRING("ENDR")
}

// TODO finner ut hvilken kode vi skal bruke her
enum class FagOmrådeKode(val kode : String) {
    HELSE("HELSE")
}

enum class UtbetalingsfrekvensKode(val kode : String ) {
    DAGLIG("DAG"),
    UKENTLIG("UKE"),
    MÅNEDLIG("MND"),
    DAGLIG_14("14DG"),
    ENGANGSUTBETALING("ENG")
}

/*
   Kode-komponent definerer hvilket system som har sendt data til Oppdrag.
 */
enum class KomponentKode(val kode: String) {
    VLSP("VLSP") // finner ut
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
    A_KONTO("AKTO")
}
