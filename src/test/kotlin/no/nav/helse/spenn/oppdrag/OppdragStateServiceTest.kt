package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import java.util.*
import kotlin.test.assertEquals

@JooqTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])
@ComponentScan(basePackages = ["no.nav.helse.spenn.oppdrag.dao"])
class OppdragStateServiceTest {

    @Autowired
    lateinit var service: OppdragStateService

    val mockSimuleringService = mock(SimuleringService::class.java)




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

}