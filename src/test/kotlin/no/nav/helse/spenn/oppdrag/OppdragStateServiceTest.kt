package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.jooq.JooqTest
//import org.springframework.context.annotation.ComponentScan
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/*@JooqTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])*/
//@ComponentScan(basePackages = ["no.nav.helse.spenn.oppdrag.dao"])
class OppdragStateServiceTest {

    //@Autowired
    lateinit var service: OppdragStateService

    @Test
    fun saveAndFetch() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")
        for (i in 1..100) {
            service.saveOppdragState(OppdragStateDTO(soknadId = UUID.randomUUID(), utbetalingsOppdrag = utbetaling))
        }
        assertEquals(service.fetchOppdragStateByStatus(OppdragStateStatus.STARTET,10).size, 10, "size should be equal")
    }

    @Test
    fun insert_fetch_compare_update_refetch_compare() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")
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