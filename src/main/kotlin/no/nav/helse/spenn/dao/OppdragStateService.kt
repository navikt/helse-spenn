package no.nav.helse.spenn.dao

import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OppdragStateService(val repository: OppdragStateRepository) {


    @Transactional(readOnly = false)
    fun saveOppdragState(dto: OppdragStateDTO): OppdragStateDTO {
        if (dto.id==null) {
            return toDTO(repository.insert(toEntity(dto)))
        }
        return toDTO(repository.update(toEntity(dto)))
    }

    @Transactional(readOnly = true)
    fun fetchOppdragState(soknadId: UUID): OppdragStateDTO {
        return toDTO(repository.findBySoknadId(soknadId))
    }


    @Transactional(readOnly = true)
    fun fetchOppdragStateByStatus(status: OppdragStateStatus): List<OppdragStateDTO> {
        return repository.findAllByStatus(status).map { toDTO(it) }
    }

    @Transactional(readOnly = true)
    fun fetchOppdragStateById(id: Long): OppdragStateDTO {
        return toDTO(repository.findById(id))
    }

    private fun toEntity(dto: OppdragStateDTO): OppdragState {
        return OppdragState(id=dto.id,
                utbetalingsOppdrag = defaultObjectMapper.writeValueAsString(dto.utbetalingsOppdrag),
                soknadId = dto.soknadId,
                modified = dto.modified,
                created = dto.created,
                simuleringResult = defaultObjectMapper.writeValueAsString(dto.simuleringResult),
                status = dto.status,
                oppdragResponse = defaultObjectMapper.writeValueAsString(dto.oppdragResponse),
                avstemmingsNokkel = dto.avstemmingsNokkel

        )
    }

    private fun toDTO(entity: OppdragState): OppdragStateDTO {
        return OppdragStateDTO(id = entity.id,
                soknadId = entity.soknadId,
                status = entity.status,
                utbetalingsOppdrag = defaultObjectMapper.readValue(entity.utbetalingsOppdrag, UtbetalingsOppdrag::class.java),
                oppdragResponse = entity.oppdragResponse,
                simuleringResult = defaultObjectMapper.readValue(entity.simuleringResult, SimuleringResult::class.java),
                modified = entity.modified,
                created = entity.created,
                avstemmingsNokkel = entity.avstemmingsNokkel)
    }

}