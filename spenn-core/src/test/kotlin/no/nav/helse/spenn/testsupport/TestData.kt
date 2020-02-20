package no.nav.helse.spenn.testsupport

import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.Utbetaling
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.simulering.Simulering
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

val simuleringsresultat = SimuleringResult(
    status = SimuleringStatus.OK, simulering = Simulering(
        gjelderId = "12345678900",
        gjelderNavn = "Foo Bar", datoBeregnet = LocalDate.now(),
        totalBelop = BigDecimal.valueOf(1000), periodeList = emptyList()
    )
)

internal fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
    val orgNr = "123456789"
    return UtbetalingsOppdrag(
        behov = """{ "somejson" : 123 }""",
        utbetalingsreferanse = "1",
        oppdragGjelder = "01010112345",
        saksbehandler = "Z999999",
        utbetaling = Utbetaling(
            maksdato = LocalDate.of(2011, 12, 20),
            organisasjonsnummer = orgNr,
            utbetalingsLinjer = listOf(
                UtbetalingsLinje(
                    id = "1",
                    grad = BigInteger.valueOf(100),
                    datoFom = LocalDate.of(2011, 1, 1),
                    datoTom = LocalDate.of(2011, 1, 31),
                    utbetalesTil = orgNr,
                    sats = BigDecimal.valueOf(1000.0),
                    satsTypeKode = SatsTypeKode.DAGLIG
                )
            )
        )
    )
}

internal fun etUtbetalingsUtvidelsesOppdrag(): UtbetalingsOppdrag {
    val orgNr = "123456789"
    return UtbetalingsOppdrag(
            behov = """{ "someOtherjson" : 123 }""",
            utbetalingsreferanse = "1",
            oppdragGjelder = "01010112345",
            saksbehandler = "Z999999",
            utbetaling = Utbetaling(
                    maksdato = LocalDate.of(2011, 12, 20),
                    organisasjonsnummer = orgNr,
                    utbetalingsLinjer = listOf(
                            UtbetalingsLinje(
                                    id = "1",
                                    grad = BigInteger.valueOf(100),
                                    datoFom = LocalDate.of(2011, 1, 1),
                                    datoTom = LocalDate.of(2011, 2, 28),
                                    utbetalesTil = orgNr,
                                    sats = BigDecimal.valueOf(1000.0),
                                    satsTypeKode = SatsTypeKode.DAGLIG
                            )
                    )
            )
    )
}

fun etEnkeltBehov() = defaultObjectMapper.readTree("""
{
  "@behov": "Utbetaling",
  "sakskompleksId": "e25ccad5-f5d5-4399-bb9d-43e9fc487888",
  "utbetalingsreferanse": "1",
  "fødselsnummer": "12345678901",
  "aktørId": "1234567890123",
  "organisasjonsnummer": "123456789",
  "maksdato": "2011-12-20",
  "saksbehandler": "Z999999",
  "utbetalingslinjer": [
    {
      "fom": "2011-01-01",
      "tom": "2011-01-31",
      "grad": 100,
      "dagsats": "1000.0"
    }
  ]
}        
    """.trimIndent())

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
