package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.withMDC
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class Logg(private val offentlig: Logger, private val privat: Logger, private val fellesKontekst: Map<String, String> = emptyMap()) {
    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        fun ny(klassen: KClass<*>) = Logg(
            offentlig = LoggerFactory.getLogger(klassen.java),
            privat = sikkerLogg
        )
    }
    fun fellesKontekst(mdc: Map<String, String>) = Logg(offentlig, privat, fellesKontekst + mdc)

    fun info(melding: String, vararg arg: Any) = apply {
        withMDC(fellesKontekst) {
            offentlig.info(melding, *arg)
            privat.info(melding, *arg)
        }
    }

    fun error(melding: String, vararg arg: Any) = apply {
        withMDC(fellesKontekst) {
            offentlig.error(melding, *arg)
            privat.error(melding, *arg)
        }
    }

    fun offentligError(melding: String, vararg arg: Any) = apply {
        withMDC(fellesKontekst) {
            offentlig.error(melding, *arg)
        }
    }

    fun privatError(melding: String, vararg arg: Any) = apply {
        withMDC(fellesKontekst) {
            privat.error(melding, *arg)
        }
    }
}