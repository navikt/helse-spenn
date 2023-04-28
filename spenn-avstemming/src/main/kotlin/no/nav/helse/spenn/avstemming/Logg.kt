package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.withMDC
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class Logg(
    private val offentlig: Logger,
    private val privat: Logger,
    private val åpenKontekst: Map<String, String> = emptyMap(),
    private val privatKontekst: Map<String, String> = emptyMap()
) {
    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        fun ny(klassen: KClass<*>) = Logg(
            offentlig = LoggerFactory.getLogger(klassen.java),
            privat = sikkerLogg
        )
    }
    fun åpent(key: String, value: String) = Logg(offentlig, privat, åpenKontekst + (key to value), privatKontekst)
    fun privat(key: String, value: String) = Logg(offentlig, privat, åpenKontekst, privatKontekst + (key to value))

    fun info(melding: String, vararg arg: Any) = apply {
        offentligInfo(melding, *arg)
        privatInfo(melding, *arg)
    }

    fun offentligInfo(melding: String, vararg arg: Any) = apply {
        withMDC(åpenKontekst) {
            offentlig.info(melding, *arg)
        }
    }
    fun privatInfo(melding: String, vararg arg: Any) = apply {
        withMDC(åpenKontekst + privatKontekst) {
            privat.info(melding, *arg)
        }
    }

    fun error(melding: String, vararg arg: Any) = apply {
        offentligError(melding, *arg)
        privatError(melding, *arg)
    }

    fun offentligError(melding: String, vararg arg: Any) = apply {
        withMDC(åpenKontekst) {
            offentlig.error(melding, *arg)
        }
    }

    fun privatError(melding: String, vararg arg: Any) = apply {
        withMDC(åpenKontekst + privatKontekst) {
            privat.error(melding, *arg)
        }
    }
}