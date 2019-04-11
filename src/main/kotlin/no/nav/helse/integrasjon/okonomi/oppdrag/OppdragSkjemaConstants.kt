package no.nav.helse.integrasjon.okonomi.oppdrag

import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


class OppdragSkjemaConstants {

    companion object {
        @JvmField
        val JAXB_CLASS = Oppdrag::class.java
        const val SP_ENHET = "4151"
        const val BOS = "BOS"
        const val KOMPONENT_KODE = "SPREFAG-IOP"
        const val APP = "SPENN"
        const val SP = "SP"

        @JvmStatic
        fun toXMLDate(dato : LocalDate) : XMLGregorianCalendar {
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(GregorianCalendar.from(dato.atStartOfDay(ZoneId.systemDefault())))
        }

        @JvmStatic
        fun toFnrOrOrgnr(fonr: String) : String {
            if (fonr.length==9) return "00" + fonr
            return fonr
        }

    }

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

enum class KvitteringTransaksjonStatus(val kode : String) {
    OK("00"),
    AKSEPTERT_MEN_NOE_ER_FEIL("04"),
    AVVIST_FUNKSJONELLE_FEIL("08"),
    AVVIST_TEKNISK_FEIL("12")
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