package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.simulering.SimuleringResult
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


class OppdragStateService(private val repository: OppdragStateRepository) {

    inner class Transaksjon(dto: TransaksjonDTO) {
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
    // Trengs eventuelt kun for å gjøre OPPHør basert på utbetalingsreferanse
/*
    fun fetchOppdragState(utbetalingsreferanse: String): TransaksjonDTO {
        return toDTO(repository.findByUtbetalingsreferanse(utbetalingsreferanse))
    }

 */

    // For å hente ut for å lagre respons fra OS
    fun hentTransaksjon(utbetalingsreferanse: String, avstemmingsNøkkel: LocalDateTime) =
        Transaksjon(repository.findByRefAndNokkel(utbetalingsreferanse, avstemmingsNøkkel))

    // Bare test ????
    /*fun fetchOppdragStateByAvstemtAndStatus(avstemt: Boolean, status: OppdragStateStatus): List<TransaksjonDTO> {
        return repository.findAllByAvstemtAndStatus(avstemt, status).map { toDTO(it) }
    }*/


    /*
    // Brukes av SendTilOSTask (status==SIMULERING_OK) og SEndTilSimuleringTask (status==STARTET)
    fun fetchOppdragStateByStatus(status: OppdragStateStatus, limit: Int = 100): List<TransaksjonDTO> {
        return repository.findAllByStatus(status, limit).map { toDTO(it) }
    }

    // Brukes av avstemmings-tasken
    fun fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(avstemmingsnokkelMax: LocalDateTime): List<TransaksjonDTO> {
        return repository.findAllNotAvstemtWithAvstemmingsnokkelNotAfter(avstemmingsnokkelMax).map { toDTO(it) }
    }*/


    fun oppdaterSimuleringsresultat(transaksjon: TransaksjonDTO, result: SimuleringResult, status: OppdragStateStatus) {

    }

    fun hentNyeBehov() =
        repository.findAllByStatus(TransaksjonStatus.STARTET).map { Transaksjon(it) }

    fun hentFerdigsimulerte(limit: Int = 100) =
        repository.findAllByStatus(TransaksjonStatus.SIMULERING_OK, limit).map { Transaksjon(it) }

    fun hentEnnåIkkeAvstemteTransaksjonerEldreEnn(maks: LocalDateTime) =
        repository.findAllNotAvstemtWithAvstemmingsnokkelNotAfter(maks).map { Transaksjon(it) }



    // Brukes kun i test
    /*fun fetchOppdragStateById(id: Long): TransaksjonDTO {
        return toDTO(repository.findById(id))
    }*/


}

/*fun toEntity(dto: TransaksjonDTO): OppdragState {
    return OppdragState(id = dto.id,
            utbetalingsOppdrag = defaultObjectMapper.writeValueAsString(dto.utbetalingsOppdrag),
            sakskompleksId = dto.sakskompleksId,
            utbetalingsreferanse = dto.utbetalingsreferanse,
            modified = dto.modified,
            created = dto.created,
            simuleringResult = defaultObjectMapper.writeValueAsString(dto.simuleringResult),
            status = dto.status,
            oppdragResponse = dto.oppdragResponse,
            feilbeskrivelse = dto.feilbeskrivelse,
            avstemming = toAvstemmingEntity(dto.avstemming)
    )
}

fun toDTO(entity: OppdragState): TransaksjonDTO {
    return TransaksjonDTO(id = entity.id,
            sakskompleksId = entity.sakskompleksId,
            utbetalingsreferanse = entity.utbetalingsreferanse,
            status = entity.status,
            utbetalingsOppdrag = defaultObjectMapper.readValue(entity.utbetalingsOppdrag, UtbetalingsOppdrag::class.java),
            oppdragResponse = entity.oppdragResponse,
            simuleringResult = entity.simuleringResult?.let { defaultObjectMapper.readValue(it, SimuleringResult::class.java) },
            modified = entity.modified,
            created = entity.created,
            feilbeskrivelse = entity.feilbeskrivelse,
            avstemming = toAvstemmingDTO(entity.avstemming))
}

fun toAvstemmingEntity(dto: AvstemmingDTO?): Avstemming? {
    if (dto==null) return null
    return Avstemming(id = dto.id, oppdragstateId = dto.oppdragStateId, nokkel = dto.nokkel, avstemt = dto.avstemt)
}

fun toAvstemmingDTO(entity: Avstemming?): AvstemmingDTO? {
    if (entity == null) return null
    return AvstemmingDTO(id = entity.id, oppdragStateId = entity.oppdragstateId,
            nokkel = entity.nokkel, avstemt = entity.avstemt)
}*/

internal class SanityCheckException(message : String) : Exception(message)

// TODO ? Konfig? Sykepenger er maks 6G, maksTillattDagsats kan ikke være lavere enn dette.
private fun maksTillattDagsats(G: Int = 100_000, hverdagerPrÅr: Int = 260) = BigDecimal(6.5 * G / hverdagerPrÅr)
