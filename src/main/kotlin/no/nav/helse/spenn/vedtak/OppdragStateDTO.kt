package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.OppdragResponse
import no.nav.helse.spenn.simulering.SimuleringResult
import java.time.LocalDateTime
import java.util.*

data class OppdragStateDTO (val id: Long? = null, val soknadId: UUID, val created: LocalDateTime = LocalDateTime.now(),
                            val modified: LocalDateTime = LocalDateTime.now(),
                            val vedtak: Vedtak, var status: OppdragStateStatus = OppdragStateStatus.PENDING,
                            var oppdragResponse: OppdragResponse? = null, var simuleringResult: SimuleringResult? = null)