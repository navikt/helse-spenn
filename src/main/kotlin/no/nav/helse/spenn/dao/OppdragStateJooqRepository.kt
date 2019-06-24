package no.nav.helse.spenn.dao

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import no.nav.helse.spenn.jooq.Tables.OPPDRAGSTATE
import no.nav.helse.spenn.jooq.tables.records.OppdragstateRecord
import org.jooq.impl.DSL.currentTimestamp
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*


@Repository
class OppdragStateJooqRepository(val jooq: DSLContext): OppdragStateRepository {


    @Transactional(readOnly = false)
    override fun insert(oppdragstate: OppdragState): OppdragState {
        with(OPPDRAGSTATE) {
            val id =  jooq.insertInto(this)
                    .set(SOKNAD_ID, oppdragstate.soknadId)
                    .set(MODIFIED, currentTimestamp())
                    .set(CREATED, currentTimestamp())
                    .set(UTBETALINGSOPPDRAG, oppdragstate.utbetalingsOppdrag)
                    .set(STATUS, oppdragstate.status.name)
                    .set(OPPDRAGRESPONSE, oppdragstate.oppdragResponse)
                    .set(SIMULERINGRESULT, oppdragstate.simuleringResult)
                    .set(AVSTEMMINGSNOKKEL, oppdragstate.avstemmingsNokkel)
                    .returning()
                    .fetchOne()
                    .id
            return findById(id)
        }
    }

    @Transactional(readOnly = false)
    override fun delete(id: Long): OppdragState {
        val delete = findById(id)
        jooq.delete(OPPDRAGSTATE)
                .where(OPPDRAGSTATE.ID.equal(id))
                .execute()
        return delete
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<OppdragState> {
        return jooq.selectFrom(OPPDRAGSTATE)
                .fetchInto(OppdragstateRecord::class.java)
                .map { it.toOppdragState() }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long?): OppdragState {
        return jooq.selectFrom(OPPDRAGSTATE)
                .where(OPPDRAGSTATE.ID.equal(id))
                .fetchOne()
                .toOppdragState()
    }

    @Transactional(readOnly = true)
    override fun findAllByStatus(status: OppdragStateStatus): List<OppdragState> {
        return jooq.selectFrom(OPPDRAGSTATE)
                .where(OPPDRAGSTATE.STATUS.equal(status.name))
                .map { it.toOppdragState() }
    }

    @Transactional(readOnly = false)
    override fun update(oppdragstate: OppdragState): OppdragState {
        with(OPPDRAGSTATE) {
            jooq.update(this)
                    .set(SOKNAD_ID, oppdragstate.soknadId)
                    .set(MODIFIED, currentTimestamp())
                    .set(STATUS, oppdragstate.status.name)
                    .set(UTBETALINGSOPPDRAG, oppdragstate.utbetalingsOppdrag)
                    .set(SIMULERINGRESULT, oppdragstate.simuleringResult)
                    .set(OPPDRAGRESPONSE, oppdragstate.oppdragResponse)
                    .set(AVSTEMMINGSNOKKEL, oppdragstate.avstemmingsNokkel)
                    .where(ID.equal(oppdragstate.id))
                    .execute()
        }
        return findById(oppdragstate.id)
    }

    @Transactional(readOnly = true)
    override fun findBySoknadId(soknadId: UUID): OppdragState {
        return jooq.selectFrom(OPPDRAGSTATE)
                .where(OPPDRAGSTATE.SOKNAD_ID.equal(soknadId))
                .fetchOne()
                .toOppdragState()
    }
}

private fun LocalDateTime?.toTimeStamp(): Timestamp? {
   return  if (this != null ) Timestamp.valueOf(this) else null
}

private fun OppdragstateRecord?.toOppdragState(): OppdragState {
    if (this==null) throw OppdragStateNotFound()
    return OppdragState(id=id, soknadId = soknadId, created = created.toLocalDateTime(), modified = modified.toLocalDateTime(),
            utbetalingsOppdrag = utbetalingsoppdrag, oppdragResponse = oppdragresponse, status = OppdragStateStatus.valueOf(status),
            simuleringResult = simuleringresult, avstemmingsNokkel = avstemmingsnokkel)
}


class OppdragStateNotFound : Throwable()
