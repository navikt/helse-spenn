package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppdrag.oppdragRequest
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDateTime

class TransaksjonService internal constructor(private val repository: TransaksjonRepository) {

    inner class Transaksjon internal constructor(dto: TransaksjonDTO) {
        private var transaksjonDTO = dto

        val oppdragRequest: Oppdrag get() {
            require(transaksjonDTO.status == TransaksjonStatus.SENDT_OS)
            return transaksjonDTO.oppdragRequest
        }

        fun forberedSendingTilOS() {
            performSanityCheck()
            // TODO: Sett status = SENDT_OS + ny avstemmingsnøkkel
            val updated = transaksjonDTO.copy(status = TransaksjonStatus.SENDT_OS, nokkel = LocalDateTime.now())
            transaksjonDTO = updated
        }

        fun lagreOSResponse(status: TransaksjonStatus, xml: String, feilmelding: String?) {
            repository.lagreOSResponse(transaksjonDTO.utbetalingsreferanse, transaksjonDTO.nokkel!!, status, xml, feilmelding)
            transaksjonDTO = repository.findByRefAndNokkel(transaksjonDTO.utbetalingsreferanse, transaksjonDTO.nokkel!!)
        }

        override fun toString() = "Transaksjon(sakskompleksId=${transaksjonDTO.sakskompleksId}, utbetalingsreferanse=${transaksjonDTO.utbetalingsreferanse}, " +
                "avstemmingsnøkkel=${transaksjonDTO.nokkel})"

        private fun performSanityCheck() {
            transaksjonDTO.utbetalingsOppdrag.utbetalingsLinje.forEach {
                if (it.satsTypeKode != SatsTypeKode.DAGLIG) {
                    throw SanityCheckException("satsTypeKode er ${it.satsTypeKode}. Vi har ikke logikk for å sanity-sjekke dette.")
                }
                val maksDagsats = maksTillattDagsats()
                if (it.sats > maksDagsats) {
                    throw SanityCheckException("dagsats ${it.sats} som er høyere enn begrensningen på $maksDagsats")
                }
            }
        }

    }

    fun annulerUtbetaling(oppdrag: UtbetalingsOppdrag) {
        require(oppdrag.annulering == true)
        val transaksjoner = repository.findByRef(oppdrag.behov.utbetalingsreferanse)
        require(1 == transaksjoner.size)
        require(transaksjoner.first().sakskompleksId == oppdrag.behov.sakskompleksId)
        repository.insertNyTransaksjon(oppdrag)
    }

    fun lagreNyttOppdrag(oppdrag: UtbetalingsOppdrag) {
        require(oppdrag.annulering == false)
        repository.insertNyttOppdrag(oppdrag)
    }

    // For å hente ut for å lagre respons fra OS
    fun hentTransaksjon(utbetalingsreferanse: String, avstemmingsNøkkel: LocalDateTime) =
        Transaksjon(repository.findByRefAndNokkel(utbetalingsreferanse, avstemmingsNøkkel))

    fun hentNyeBehov() =
        repository.findAllByStatus(TransaksjonStatus.STARTET).map { Transaksjon(it) }

    fun hentFerdigsimulerte(limit: Int = 100) =
        repository.findAllByStatus(TransaksjonStatus.SIMULERING_OK, limit).map { Transaksjon(it) }

    fun hentEnnåIkkeAvstemteTransaksjonerEldreEnn(maks: LocalDateTime) =
        repository.findAllNotAvstemtWithAvstemmingsnokkelNotAfter(maks).map { Transaksjon(it) }

}

internal class SanityCheckException(message : String) : Exception(message)

// TODO ? Konfig? Sykepenger er maks 6G, maksTillattDagsats kan ikke være lavere enn dette.
private fun maksTillattDagsats(G: Int = 100_000, hverdagerPrÅr: Int = 260) = BigDecimal(6.5 * G / hverdagerPrÅr)
