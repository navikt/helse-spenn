package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.SimuleringResult
import java.time.LocalDateTime
import java.util.*

data class OppdragStateDTO (val id: Long? = null, val soknadId: UUID, val created: LocalDateTime = LocalDateTime.now(),
                            val modified: LocalDateTime = LocalDateTime.now(),
                            val utbetalingsOppdrag: UtbetalingsOppdrag, var status: OppdragStateStatus = OppdragStateStatus.STARTET,
                            var oppdragResponse: String? = null, var simuleringResult: SimuleringResult? = null)