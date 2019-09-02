package no.nav.helse.spenn.simulering

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class SimuleringResultTest {

    @Test
    fun simuleringResultTest() {
        val result = SimuleringResult(status = Status.OK, feilMelding = "", mottaker = Mottaker(datoBeregnet = "2019-12-01", gjelderId = "123456",
                gjelderNavn = "gjelder navn", totalBelop = BigDecimal.ONE, periodeList = listOf(Periode(id = "delytelseId",
                faktiskFom = LocalDate.now(), faktiskTom = LocalDate.now(), oppdragsId = 123L, forfall = LocalDate.now(),
                utbetalesTilId = "12345", utbetalesTilNavn = "navn", konto = "12345", belop = BigDecimal.ONE,
                sats = BigDecimal.ONE, typeSats = SatsTypeKode.MÅNEDLIG, antallSats = BigDecimal.ONE, uforegrad = BigInteger.ONE,
                utbetalingsType = UtbetalingsType.YTELSE, tilbakeforing = true, behandlingsKode ="kode")),kodeFaggruppe = "KODE")
        )
        val resultAsString = defaultObjectMapper.writeValueAsString(result)
        val resultDes = defaultObjectMapper.writeValueAsString(defaultObjectMapper.readTree(simuleringResultv1))
        val simuleringResult = defaultObjectMapper.readValue(simuleringResultv1,SimuleringResult::class.java)
        println(simuleringResult.mottaker?.kodeFaggruppe)
        println(simuleringResult.mottaker!!.periodeList[0].tilbakeforing)
        //assertEquals(resultAsString, resultDes)


    }
}

val simuleringResultv1 = """{"status":"OK","feilMelding":"","mottaker":{"gjelderId":"123456","gjelderNavn":"gjelder navn","datoBeregnet":"2019-12-01","totalBelop":1,"periodeList":[{"id":"delytelseId","faktiskFom":"2019-09-02","faktiskTom":"2019-09-02","oppdragsId":123,"forfall":"2019-09-02","utbetalesTilId":"12345","utbetalesTilNavn":"navn","konto":"12345","belop":1,"sats":1,"typeSats":"MÅNEDLIG","antallSats":1,"uforegrad":1,"utbetalingsType":"YTELSE"}]}}
"""