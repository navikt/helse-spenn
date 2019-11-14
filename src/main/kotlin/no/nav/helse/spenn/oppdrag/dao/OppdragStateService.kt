package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.AvstemmingDTO
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.oppdrag.toFagId
import no.nav.helse.spenn.simulering.SimuleringResult
import java.time.LocalDateTime
import java.util.*

class OppdragStateService(val repository: OppdragStateRepository) {

    //@Transactional(readOnly = false)
    fun saveOppdragState(dto: OppdragStateDTO): OppdragStateDTO {
        if (dto.id==null) {
            return toDTO(repository.insert(toEntity(dto)))
        }
        return toDTO(repository.update(toEntity(dto)))
    }

    //@Transactional(readOnly = true)
    fun fetchOppdragState(soknadId: UUID): OppdragStateDTO {
        return toDTO(repository.findBySoknadId(soknadId))
    }

    //@Transactional(readOnly = true)
    fun fetchOppdragStateByAvstemtAndStatus(avstemt: Boolean, status: OppdragStateStatus): List<OppdragStateDTO> {
        return repository.findAllByAvstemtAndStatus(avstemt, status).map { toDTO(it) }
    }

    //@Transactional(readOnly = true)
    fun fetchOppdragStateByStatus(status: OppdragStateStatus, limit: Int = 100): List<OppdragStateDTO> {
        return repository.findAllByStatus(status, limit).map { toDTO(it) }
    }

    //@Transactional(readOnly = true)
    fun fetchOppdragStateByNotAvstemtAndMaxAvstemmingsnokkel(avstemmingsnokkelMax: LocalDateTime): List<OppdragStateDTO> {
        return repository.findAllNotAvstemtWithAvstemmingsnokkelNotAfter(avstemmingsnokkelMax).map { toDTO(it) }
    }

    //@Transactional(readOnly = true)
    fun fetchOppdragStateById(id: Long): OppdragStateDTO {
        return toDTO(repository.findById(id))
    }


}

fun toEntity(dto: OppdragStateDTO): OppdragState {
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

fun toDTO(entity: OppdragState): OppdragStateDTO {
    return OppdragStateDTO(id = entity.id,
            sakskompleksId = entity.sakskompleksId,
            utbetalingsreferanse = entity.utbetalingsreferanse,
            status = entity.status,
            utbetalingsOppdrag = defaultObjectMapper.readValue(entity.utbetalingsOppdrag, UtbetalingsOppdrag::class.java),
            oppdragResponse = entity.oppdragResponse,
            simuleringResult = entity.simuleringResult?.let { defaultObjectMapper.readValue(it, SimuleringResult::class.java) },
            modified = entity.modified,
            created = entity.created,
            feilbeskrivelse = entity.feilbeskrivelse,
            avstemming = toAvstemmingDTO(entity.avstemming),
            fagId = entity.sakskompleksId.toFagId())
}

fun toAvstemmingEntity(dto: AvstemmingDTO?): Avstemming? {
    if (dto==null) return null
    return Avstemming(id = dto.id, oppdragstateId = dto.oppdragStateId, nokkel = dto.nokkel, avstemt = dto.avstemt)
}

fun toAvstemmingDTO(entity: Avstemming?): AvstemmingDTO? {
    if (entity == null) return null
    return AvstemmingDTO(id = entity.id, oppdragStateId = entity.oppdragstateId,
            nokkel = entity.nokkel, avstemt = entity.avstemt)
}

