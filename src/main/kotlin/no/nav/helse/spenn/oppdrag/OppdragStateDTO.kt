package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.vedtak.Løsning
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppdragStateDTO(
    val id: Long? = null,
    val sakskompleksId: UUID,
    val created: LocalDateTime = LocalDateTime.now(),
    val modified: LocalDateTime = LocalDateTime.now(),
    val utbetalingsOppdrag: UtbetalingsOppdrag,
    val status: OppdragStateStatus = OppdragStateStatus.STARTET,
    val oppdragResponse: String? = null,
    val simuleringResult: SimuleringResult? = null,
    val feilbeskrivelse: String? = null,
    val avstemming: AvstemmingDTO? = null,
    val fagId: String = sakskompleksId.toFagId()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvstemmingDTO(
    val id: Long? = null,
    val oppdragStateId: Long? = null,
    val nokkel: LocalDateTime = LocalDateTime.now(),
    val avstemt: Boolean = false
)

fun OppdragStateDTO.tilLøstBehov() = this.utbetalingsOppdrag.behov.copy(løsning = Løsning(this.id!!))