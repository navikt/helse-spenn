package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.kWhen
import no.nav.helse.spenn.oppdrag.dao.*
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mockito.mock
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OppdragStateRepTransactionTest {


    val repository: OppdragStateRepository =
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())

    val exceptionTypeThrownWhenSoknadNotFound = NullPointerException::class.java // Probably not ideal, but thats what jooq's fetchOne() do for now

    @Test
    fun test_insertOppdragState_AtIkkeOppdragStateLagresNaarLagringAvAvstemmingenFeiler() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")

        val trasigAvstemming = mock(Avstemming::class.java)
        kWhen(trasigAvstemming.nokkel).thenThrow(UventetFeilTestException())

        val oppdragState = OppdragState(soknadId = soknadKey, status = OppdragStateStatus.STARTET,
                utbetalingsOppdrag = defaultObjectMapper.writeValueAsString(utbetaling),
                avstemming = trasigAvstemming)

        assertThrows(exceptionTypeThrownWhenSoknadNotFound, {
            repository.findBySoknadId(soknadKey)
        }, "Søknaden kan ikke finnes fra før når vi skal teste")

        assertThrows(UventetFeilTestException::class.java, {
            repository.insert(oppdragState)
        }, "Hvis ikke feilen kastes får vi ikke testet det vi vil teste")

        assertThrows(exceptionTypeThrownWhenSoknadNotFound, {
            repository.findBySoknadId(soknadKey)
        }, "oppdragState skal ikke ha blitt lagret, siden lagring av avstemming feilet")
    }

    @Test
    fun test_updateOppdragState_AtIkkeOppdragStateEndresNaarLagringAvAvstemmingenFeiler() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")

        val oppdragState = OppdragState(soknadId = soknadKey, status = OppdragStateStatus.STARTET,
                utbetalingsOppdrag = defaultObjectMapper.writeValueAsString(utbetaling))

        val oppdragStateMedID = repository.insert(oppdragState)

        val trasigAvstemming = mock(Avstemming::class.java)
        kWhen(trasigAvstemming.nokkel).thenThrow(UventetFeilTestException())

        assertThrows(UventetFeilTestException::class.java, {
            repository.update(oppdragStateMedID.copy(
                    status = OppdragStateStatus.FERDIG,
                    avstemming = trasigAvstemming))
        }, "Hvis ikke feilen kastes får vi ikke testet det vi vil teste")

        assertEquals(OppdragStateStatus.STARTET, repository.findBySoknadId(soknadKey).status,
                "oppdragState skal ikke ha blitt endret, siden lagring av avstemming feilet")

    }

    private class UventetFeilTestException : RuntimeException()

}