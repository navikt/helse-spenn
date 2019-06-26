package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.system.os.entiteter.oppdragskjema.Avstemmingsnokkel
import java.time.LocalDateTime
import java.util.*

data class OppdragStateDTO (val id: Long? = null, val soknadId: UUID, val created: LocalDateTime = LocalDateTime.now(),
                            val modified: LocalDateTime = LocalDateTime.now(),
                            val utbetalingsOppdrag: UtbetalingsOppdrag, val status: OppdragStateStatus = OppdragStateStatus.STARTET,
                            val oppdragResponse: String? = null, val simuleringResult: SimuleringResult? = null,
                            val avstemming: AvstemmingDTO? = null)

data class AvstemmingDTO(val id: Long? = null, val oppdragStateId: Long? = null,
                         val nokkel: LocalDateTime = LocalDateTime.now(), val avstemt: Boolean = false)

