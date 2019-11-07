package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.dao.*
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
import java.sql.SQLIntegrityConstraintViolationException

import java.util.*
import kotlin.test.*

class OppdragStateRepositoryTest {

    val repository: OppdragStateRepository =
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())

    @Test
    fun crudOppdragState() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetaling = behov.tilUtbetaling("12345678901")
        val state = OppdragState(soknadId = soknadKey, status = OppdragStateStatus.STARTET,
               utbetalingsOppdrag = defaultObjectMapper.writeValueAsString(utbetaling))
        val dbState = repository.insert(state)
        assertNotNull(dbState.created)
        assertNotNull(dbState.modified)
        assertEquals(soknadKey,dbState.soknadId)
        assertEquals(OppdragStateStatus.STARTET, dbState.status)
        assertNull(dbState.avstemming)

        val update = repository.update(OppdragState(
                id=dbState.id,
                soknadId = dbState.soknadId,
                utbetalingsOppdrag = dbState.utbetalingsOppdrag,
                oppdragResponse = kvittering,
                status = OppdragStateStatus.FERDIG,
                created = dbState.created,
                modified = dbState.modified,
                simuleringResult = dbState.simuleringResult,
                feilbeskrivelse = "jauda, så feil så",
                avstemming = Avstemming()))

        assertNotNull(update.avstemming)
        assertNotNull(update.avstemming?.nokkel)
        println("avstemmingId: ${update.avstemming?.id} avstemmingnokkel: ${update.avstemming?.nokkel}")
        assertEquals(OppdragStateStatus.FERDIG, update.status)
        assertEquals("jauda, så feil så", update.feilbeskrivelse)
        assertTrue(update.modified.isAfter(dbState.modified))

        try {
            repository.insert(OppdragState(soknadId = soknadKey, utbetalingsOppdrag = ""))
        } catch (e : DataAccessException) {
            println(e.stackTrace)
        }

        val exception = assertFailsWith</*DuplicateKeyException*/DataAccessException>{repository.insert(OppdragState(soknadId = soknadKey, utbetalingsOppdrag = ""))}
        assertTrue(exception.cause is SQLIntegrityConstraintViolationException)
    }

}

val kvittering = """<?xml version="1.0" encoding="utf-8"?><oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag"><mmel><systemId>231-OPPD</systemId><kodeMelding>B110008F</kodeMelding><alvorlighetsgrad>08</alvorlighetsgrad><beskrMelding>Oppdraget finnes fra før</beskrMelding><programId>K231BB10</programId><sectionNavn>CA10-INPUTKONTROLL</sectionNavn></mmel><oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SP</kodeFagomraade>
        <fagsystemId>20190408084501</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>21038014495</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01+01:00</datoOppdragGjelderFom>
        <saksbehId>SPENN</saksbehId>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>1970-01-01+01:00</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>1</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-01-01+01:00</datoVedtakFom>
            <datoVedtakTom>2019-01-12+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPENN</saksbehId>
            <utbetalesTilId>00995816598</utbetalesTilId>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>50</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPENN</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>2</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-02-13+01:00</datoVedtakFom>
            <datoVedtakTom>2019-02-20+01:00</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPENN</saksbehId>
            <utbetalesTilId>00995816598</utbetalesTilId>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>70</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPENN</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>3</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-03-18+01:00</datoVedtakFom>
            <datoVedtakTom>2019-04-12+02:00</datoVedtakTom>
            <sats>1000</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>SPENN</saksbehId>
            <utbetalesTilId>00995816598</utbetalesTilId>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>SPENN</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""

