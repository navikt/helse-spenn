package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.AvstemmingMapper
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppdrag.toOppdragRequest
import no.nav.helse.spenn.oppdrag.toSimuleringRequest
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus.OK
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
            return transaksjonDTO.toOppdragRequest()
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

        override fun toString() = "Transaksjon(utbetalingsreferanse=${transaksjonDTO.utbetalingsreferanse}, " +
                "avstemmingsnøkkel=${transaksjonDTO.nokkel})"

        private fun performSanityCheck() {
            val utbetaling = transaksjonDTO.utbetalingsOppdrag.utbetaling
            utbetaling?.utbetalingsLinjer?.forEach {
                if (it.satsTypeKode != SatsTypeKode.DAGLIG) {
                    throw SanityCheckException("satsTypeKode er ${it.satsTypeKode}. Vi har ikke logikk for å sanity-sjekke dette.")
                }
                val maksDagsats = maksTillattDagsats()
                if (it.sats > maksDagsats) {
                    throw SanityCheckException("dagsats ${it.sats} som er høyere enn begrensningen på $maksDagsats")
                }
            }
        }

        private fun erAnnulering() = transaksjonDTO.utbetalingsOppdrag.utbetaling == null

        fun oppdaterSimuleringsresultat(result: SimuleringResult) {
            if (result.simulering == null && result.status == OK) require(erAnnulering())
            val status = if (result.status == OK) TransaksjonStatus.SIMULERING_OK
                else TransaksjonStatus.SIMULERING_FEIL
            transaksjonDTO = repository.oppdaterTransaksjonMedStatusOgSimuleringResult(
                transaksjonDTO,
                status,
                defaultObjectMapper.writeValueAsString(result)
            )
        }

        fun markerSomAvstemt() {
            repository.markerSomAvstemt(transaksjonDTO)
            transaksjonDTO = transaksjonDTO.copy(avstemt = true)
        }

    }

    fun annulerUtbetaling(oppdrag: UtbetalingsOppdrag) {
        require(oppdrag.utbetaling == null)
        val transaksjoner = repository.findByRef(oppdrag.utbetalingsreferanse)
        require(1 == transaksjoner.size)
        val originaltOppdrag = transaksjoner.first().utbetalingsOppdrag
        require(oppdrag.oppdragGjelder == originaltOppdrag.oppdragGjelder)

        requireNotNull(originaltOppdrag.utbetaling)
        require(originaltOppdrag.utbetaling.utbetalingsLinjer.isNotEmpty())
        repository.insertNyTransaksjon(oppdrag.copy(
            statusEndringFom = originaltOppdrag.utbetaling.utbetalingsLinjer.minBy { it.datoFom }!!.datoFom,
            opprinneligOppdragTom = originaltOppdrag.utbetaling.utbetalingsLinjer.maxBy { it.datoTom }!!.datoTom
        ))
    }

    fun lagreNyttOppdrag(oppdrag: UtbetalingsOppdrag) {
        requireNotNull(oppdrag.utbetaling)
        require(oppdrag.utbetaling.utbetalingsLinjer.isNotEmpty())
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
        utbetalingsreferanse = this.utbetalingsreferanse,
        nokkel = LocalDateTime.now(),
        utbetalingsOppdrag = this).toSimuleringRequest()

fun List<OppdragService.Transaksjon>.lagAvstemmingsmeldinger() =
    AvstemmingMapper(this.map { it.dto }, FagOmraadekode.SYKEPENGER_REFUSJON).lagAvstemmingsMeldinger()

class SanityCheckException(message : String) : Exception(message)

// TODO ? Konfig? Sykepenger er maks 6G, maksTillattDagsats kan ikke være lavere enn dette.
private fun maksTillattDagsats(G: Int = 100_000, hverdagerPrÅr: Int = 260) = BigDecimal(6.5 * G / hverdagerPrÅr)
