package no.nav.helse.spenn.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.lang.IllegalArgumentException
import java.time.format.DateTimeFormatter

enum class FagOmraadekode(val kode: String) {
    SYKEPENGER("SP"),
    SYKEPENGER_REFUSJON("SPREF")
}

enum class KvitteringAlvorlighetsgrad(val kode : String) {
    OK("00"),
    AKSEPTERT_MEN_NOE_ER_FEIL("04"),
    AVVIST_FUNKSJONELLE_FEIL("08"),
    AVVIST_TEKNISK_FEIL("12");

    companion object {
        fun fromKode(kode: String) : KvitteringAlvorlighetsgrad {
            KvitteringAlvorlighetsgrad.values().forEach {
                if (it.kode == kode) return it
            }
            throw IllegalArgumentException("No enum constant with kode=$kode")
        }
    }

}

val avstemmingsnokkelFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

internal val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)