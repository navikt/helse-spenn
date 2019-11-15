package no.nav.helse.spenn.oppdrag.dao

import java.time.LocalDateTime

interface OppdragStateRepository {

    fun insert(oppdragstate: OppdragState): OppdragState

    fun delete(id: Long): OppdragState

    fun findAll(): List<OppdragState>

    fun findAllByStatus(status: OppdragStateStatus, limit: Int): List<OppdragState>

    fun findAllByAvstemtAndStatus(avstemt: Boolean, status: OppdragStateStatus): List<OppdragState>

    fun findAllNotAvstemtWithAvstemmingsnokkelNotAfter(avstemmingsnokkelMax: LocalDateTime): List<OppdragState>

    fun findById(id: Long?): OppdragState

    fun findByUtbetalingsreferanse(utbetalingsreferanse: String) : OppdragState

    fun update(oppdragstate: OppdragState): OppdragState
}