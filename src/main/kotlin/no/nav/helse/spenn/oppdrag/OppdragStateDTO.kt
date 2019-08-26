package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.simulering.SimuleringResult
import java.time.LocalDateTime
import java.util.*

data class OppdragStateDTO (val id: Long? = null, val soknadId: UUID, val created: LocalDateTime = LocalDateTime.now(),
                            val modified: LocalDateTime = LocalDateTime.now(),
                            val utbetalingsOppdrag: UtbetalingsOppdrag, val status: OppdragStateStatus = OppdragStateStatus.STARTET,
                            val oppdragResponse: String? = null, val simuleringResult: SimuleringResult? = null,
                            val avstemming: AvstemmingDTO? = null)

data class AvstemmingDTO(val id: Long? = null, val oppdragStateId: Long? = null,
                         val nokkel: LocalDateTime = LocalDateTime.now(), val avstemt: Boolean = false)

