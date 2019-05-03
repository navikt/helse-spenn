package no.nav.helse.spenn.dao

import no.nav.helse.spenn.oppdrag.OppdragResponse
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.vedtak.OppdragStateDTO
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OppdragStateService(val repository: OppdragStateRepository) {


    @Transactional(readOnly = false)
    fun saveOppdragState(dto: OppdragStateDTO): OppdragStateDTO {
        if (dto.id==null) {
            return fromEntity(repository.insert(toEntity(dto)))
        }
        return fromEntity(repository.update(toEntity(dto)))
    }

    @Transactional(readOnly = true)
    fun fetchOppdragState(soknadId: UUID):  OppdragStateDTO {
        return fromEntity(repository.findBySoknadId(soknadId))
    }

    private fun toEntity(dto: OppdragStateDTO): OppdragState {
        return OppdragState(id=dto.id,
                vedtak = defaultObjectMapper.writeValueAsString(dto.vedtak),
                soknadId = dto.soknadId,
                modified = dto.modified,
                created = dto.created,
                simuleringResult = defaultObjectMapper.writeValueAsString(dto.simuleringResult),
                status = dto.status,
                oppdragResponse = defaultObjectMapper.writeValueAsString(dto.oppdragResponse)
        )
    }

    private fun fromEntity(entity: OppdragState): OppdragStateDTO {
        return OppdragStateDTO(id = entity.id,
                soknadId = entity.soknadId,
                status = entity.status,
                vedtak = defaultObjectMapper.readValue(entity.vedtak,Vedtak::class.java),
                oppdragResponse = defaultObjectMapper.readValue(entity.oppdragResponse, OppdragResponse::class.java),
                simuleringResult = defaultObjectMapper.readValue(entity.simuleringResult, SimuleringResult::class.java),
                modified = entity.modified,
                created = entity.created)
    }

}