package no.nav.helse.integrasjon.okonomi.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.dao.OppdragState
import no.nav.helse.spenn.dao.OppdragStateRepository
import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.OppdragResponse
import no.nav.helse.spenn.oppdrag.OppdragStatus
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.context.annotation.ComponentScan

import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DataJdbcTest
@ImportAutoConfiguration(classes = arrayOf(JooqAutoConfiguration::class))
@ComponentScan(basePackages = arrayOf("no.nav.helse.spenn.dao"))
class OppdragStateRepositoryTest {

    @Autowired lateinit var repository: OppdragStateRepository

    @Test
    fun crudOppdragState() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val vedtakAsString = defaultObjectMapper.writeValueAsString(vedtak)
        val state = OppdragState(soknadId = soknadKey, status = OppdragStateStatus.PENDING,
                vedtak = vedtakAsString)
        val dbState = repository.insert(state)
        assertNotNull(dbState.created)
        assertNotNull(dbState.modified)
        assertEquals(soknadKey,dbState.soknadId)
        assertEquals(OppdragStateStatus.PENDING, dbState.status)
        assertEquals(vedtakAsString, dbState.vedtak)

        val oppdragResponse = OppdragResponse(status = OppdragStatus.OK, alvorlighetsgrad = "00", beskrMelding = "beskrivelse",
                fagsystemId = dbState.id.toString(), kodeMelding = "kodemelding")

        val update = repository.update(OppdragState(
                id=dbState.id,
                soknadId = dbState.soknadId,
                vedtak = dbState.vedtak,
                oppdragResponse = defaultObjectMapper.writeValueAsString(oppdragResponse),
                status = OppdragStateStatus.OK,
                created = dbState.created,
                modified = dbState.modified,
                simuleringResult = dbState.simuleringResult))

        assertEquals(OppdragStateStatus.OK, update.status)
        assertTrue(update.modified.isAfter(dbState.modified))
    }

}
