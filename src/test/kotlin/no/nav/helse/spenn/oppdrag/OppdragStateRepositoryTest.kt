package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spenn.oppdrag.dao.Avstemming
import no.nav.helse.spenn.oppdrag.dao.OppdragState
import no.nav.helse.spenn.oppdrag.dao.OppdragStateRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.helse.spenn.vedtak.tilVedtak
import org.junit.jupiter.api.Test
/*import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.dao.DuplicateKeyException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional*/

import java.util.*
import kotlin.test.*

/*@DataJdbcTest(properties = ["VAULT_ENABLED=false",
    "spring.cloud.vault.enabled=false",
    "spring.test.database.replace=none"])*/
//@ImportAutoConfiguration(classes = [JooqAutoConfiguration::class])
//@ComponentScan(basePackages = ["no.nav.helse.spenn.oppdrag.dao"])
class OppdragStateRepositoryTest {

    /*@Autowired */lateinit var repository: OppdragStateRepository

    @Test
    //@Transactional(propagation = Propagation.NEVER)
    fun crudOppdragState() {
        val soknadKey = UUID.randomUUID()
        val node = ObjectMapper().readTree(this.javaClass.getResource("/en_behandlet_soknad.json"))
        val vedtak = node.tilVedtak(soknadKey.toString())
        val utbetaling = vedtak.tilUtbetaling("12345678901")
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

        assertTrue(false, "FIXME")
        //assertFailsWith<DuplicateKeyException>{repository.insert(OppdragState(soknadId = soknadKey, utbetalingsOppdrag = ""))}
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

