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

@DataJdbcTest
@ImportAutoConfiguration(classes = arrayOf(JooqAutoConfiguration::class))
@ComponentScan(basePackages = arrayOf("no.nav.helse.spenn.dao"))
class OppdragStateRepositoryTest {

    @Autowired lateinit var repository: OppdragStateRepository

    @Test
    fun crudOppdragState() {
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(UUID.randomUUID().toString())

        val state = OppdragState(soknadId = UUID.randomUUID(), status = OppdragStateStatus.PENDING,
                vedtak = defaultObjectMapper.writeValueAsString(vedtak))
        val id = repository.insert(state)
        var dbState = repository.findById(id)
        val oppdragResponse = OppdragResponse(status = OppdragStatus.OK, alvorlighetsgrad = "00", beskrMelding = "beskrivelse",
                fagsystemId = id.toString(), kodeMelding = "kodemelding")
        val update = repository.update(OppdragState(
                id=dbState.id,
                soknadId = dbState.soknadId,
                vedtak = dbState.vedtak,
                oppdragResponse = defaultObjectMapper.writeValueAsString(oppdragResponse),
                status = OppdragStateStatus.OK,
                created = dbState.created,
                modified = dbState.modified,
                simuleringResult = dbState.simuleringResult))
        println(update)
    }

}
