package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.oppdragRequest
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDateTime

class OppdragService(dataSource: HikariDataSource) {

    private val repository = TransaksjonRepository(dataSource)

    inner class Transaksjon internal constructor(dto: TransaksjonDTO) {
        private var transaksjonDTO = dto

        internal val dto: TransaksjonDTO get() = transaksjonDTO

        val oppdragRequest: Oppdrag get() {
            require(transaksjonDTO.status == TransaksjonStatus.SENDT_OS)
            return transaksjonDTO.oppdragRequest
        }

        val simuleringRequest: SimulerBeregningRequest get() {
            require(transaksjonDTO.status == TransaksjonStatus.STARTET)
            return transaksjonDTO.toSimuleringRequest()
        }

        fun forberedSendingTilOS() {
            performSanityCheck()
            transaksjonDTO = repository.oppdaterTransaksjonMedStatusOgNøkkel(transaksjonDTO, TransaksjonStatus.SENDT_OS, LocalDateTime.now())
        }

        fun stopp(feilmelding: String?) {
            transaksjonDTO = repository.stoppTransaksjon(transaksjonDTO, feilmelding)
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

        fun oppdaterSimuleringsresultat(result: SimuleringResult) {
            val status = if (result.status == SimuleringStatus.OK) TransaksjonStatus.SIMULERING_OK
                else TransaksjonStatus.SIMULERING_FEIL
            transaksjonDTO = repository.oppdaterTransaksjonMedStatusOgSimuleringResult(transaksjonDTO, status,
                defaultObjectMapper.writeValueAsString(result))
        }

        fun markerSomAvstemt() {
            repository.markerSomAvstemt(transaksjonDTO)
            transaksjonDTO = transaksjonDTO.copy(avstemt = true)
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
        require(!oppdrag.annulering)
        repository.insertNyttOppdrag(oppdrag)
    }

    // For å hente ut for å lagre respons fra OS
    fun hentTransaksjon(utbetalingsreferanse: String, avstemmingsNøkkel: LocalDateTime) =
        Transaksjon(repository.findByRefAndNokkel(utbetalingsreferanse, avstemmingsNøkkel))

    fun hentNyeOppdrag(limit: Int) =
        repository.findAllByStatus(TransaksjonStatus.STARTET, limit).map { Transaksjon(it) }

    fun hentFerdigsimulerte(limit: Int) =
        repository.findAllByStatus(TransaksjonStatus.SIMULERING_OK, limit).map { Transaksjon(it) }

    fun hentEnnåIkkeAvstemteTransaksjonerEldreEnn(maks: LocalDateTime) =
        repository.findAllNotAvstemtWithAvstemmingsnokkelNotAfter(maks).map { Transaksjon(it) }

}

fun UtbetalingsOppdrag.lagPåSidenSimuleringsrequest() =
    TransaksjonDTO(
        id = -1,
        sakskompleksId = this.behov.sakskompleksId,
        utbetalingsreferanse = this.behov.utbetalingsreferanse,
        nokkel = LocalDateTime.now(),
        utbetalingsOppdrag = this).toSimuleringRequest()

fun List<OppdragService.Transaksjon>.lagAvstemmingsmeldinger() =
    AvstemmingMapper(this.map { it.dto }, FagOmraadekode.SYKEPENGER_REFUSJON).lagAvstemmingsMeldinger()

class SanityCheckException(message : String) : Exception(message)

// TODO ? Konfig? Sykepenger er maks 6G, maksTillattDagsats kan ikke være lavere enn dette.
private fun maksTillattDagsats(G: Int = 100_000, hverdagerPrÅr: Int = 260) = BigDecimal(6.5 * G / hverdagerPrÅr)
