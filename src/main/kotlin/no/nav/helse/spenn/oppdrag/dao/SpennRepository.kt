package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.jooq.tables.Oppdrag.OPPDRAG
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.util.*

class SpennRepository(private val jooq: DSLContext) {


    fun insert(oppdrag: Oppdrag): Oppdrag {

    }

    fun findOppdrag(utbetalingsreferanse: String): Oppdrag {
        jooq.select()
            .from(OPPDRAG)
            .where(OPPDRAG.UTBETALINGSREFERANSE.equal(utbetalingsreferanse))
    }
}

data class Oppdrag(
    val id: Long? = null,
    val sakskompleksId: UUID,
    val utbetalingsreferanse: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val modified: LocalDateTime = LocalDateTime.now(),
    val transaksjoner: List<Transaksjon> = emptyList()
)

data class Transaksjon(
    val id: Long? = null,
    val oppdragId: Long? = null,
    val nokkel: LocalDateTime = LocalDateTime.now(),
    val avstemt: Boolean = false,
    val utbetalingsOppdrag: String,
    val status: TransaksjonStatus = TransaksjonStatus.STARTET,
    val oppdragResponse: String? = null,
    val simuleringResult: String? = null,
    val feilbeskrivelse: String? = null
)

enum class TransaksjonStatus {
    STARTET,
    SIMULERING_OK,
    SIMULERING_FEIL,
    STOPPET,
    SENDT_OS,
    FERDIG,
    FEIL
}

