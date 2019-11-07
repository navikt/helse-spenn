package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OppdragStateServiceTest {

    val service = OppdragStateService(
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())
    )

    @Test
    fun saveAndFetch() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetaling = behov.tilUtbetaling("12345678901")
        for (i in 1..100) {
            service.saveOppdragState(OppdragStateDTO(soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling))
        }
        assertEquals(service.fetchOppdragStateByStatus(OppdragStateStatus.STARTET,10).size, 10, "size should be equal")
    }

    @Test
    fun insert_fetch_compare_update_refetch_compare() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetaling = behov.tilUtbetaling("12345678901")
        val soknadId = UUID.randomUUID()
        service.saveOppdragState(OppdragStateDTO(
                soknadId = soknadId,
                status = OppdragStateStatus.SIMULERING_OK,
                utbetalingsOppdrag = utbetaling,
                feilbeskrivelse = "veryWRONG"))
        val fetched = service.fetchOppdragState(soknadId)
        assertEquals(utbetaling, fetched.utbetalingsOppdrag)
        assertEquals("veryWRONG", fetched.feilbeskrivelse)
        assertEquals(OppdragStateStatus.SIMULERING_OK, fetched.status)
        assertNotNull(fetched.id)

        service.saveOppdragState(fetched.copy(
                feilbeskrivelse = "wronger",
                status = OppdragStateStatus.STOPPET
        ))
        val updated = service.fetchOppdragState(soknadId)
        assertEquals(utbetaling, updated.utbetalingsOppdrag)
        assertEquals("wronger", updated.feilbeskrivelse)
        assertEquals(OppdragStateStatus.STOPPET, updated.status)
        assertEquals(updated.id, fetched.id)
    }


}