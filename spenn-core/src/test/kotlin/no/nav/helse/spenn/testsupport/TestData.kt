package no.nav.helse.spenn.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import no.nav.helse.spenn.vedtak.Utbetalingslinje
import java.time.LocalDate
import java.util.*

internal class TestData {

    companion object {
        fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
            val node = ObjectMapper().readTree(this::class.java.getResource("/et_utbetalingsbehov.json"))
            val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
            return behov.tilUtbetaling("01010112345")
        }
    }
}

fun etEnkeltBehov(
    sakskompleksId: UUID = UUID.randomUUID(),
    maksdato: LocalDate = LocalDate.now().plusYears(1)
) = Utbetalingsbehov(
    sakskompleksId = sakskompleksId,
    utbetalingsreferanse = "1001",
    aktørId = "en random aktørid",
    saksbehandler = "yes",
    organisasjonsnummer = "897654321",
    utbetalingslinjer = listOf(
        Utbetalingslinje(
            fom = LocalDate.of(2020, 1, 15),
            tom = LocalDate.of(2020, 1, 30),
            dagsats = 1234.toBigDecimal(),
            grad = 100
        )
    ),
    maksdato = maksdato
)

val kvittering = """
    <?xml version="1.0" encoding="utf-8"?><oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag"><mmel><systemId>231-OPPD</systemId><kodeMelding>B110008F</kodeMelding><alvorlighetsgrad>08</alvorlighetsgrad><beskrMelding>Oppdraget finnes fra før</beskrMelding><programId>K231BB10</programId><sectionNavn>CA10-INPUTKONTROLL</sectionNavn></mmel><oppdrag-110>
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
