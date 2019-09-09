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
        val oldResult = defaultObjectMapper.readValue(simuleringResultv1, SimuleringResult::class.java)
        val newResult = defaultObjectMapper.readValue(simuleringResultV2, SimuleringResult::class.java)
        println(defaultObjectMapper.writeValueAsString(oldResult))
        println(defaultObjectMapper.writeValueAsString(newResult))
    }
}

val simuleringResultv1 = """{"status":"OK","feilMelding":"","mottaker":{"gjelderId":"123456","gjelderNavn":"gjelder navn","datoBeregnet":"2019-12-01","totalBelop":1,"periodeList":[{"id":"delytelseId","faktiskFom":"2019-09-02","faktiskTom":"2019-09-02","oppdragsId":123,"forfall":"2019-09-02","utbetalesTilId":"12345","utbetalesTilNavn":"navn","konto":"12345","belop":1,"sats":1,"typeSats":"MÅNEDLIG","antallSats":1,"uforegrad":1,"utbetalingsType":"YTELSE"}]}}
"""
 val simuleringResultV2 = """{
  "status": "OK",
  "feilMelding": "",
  "simulering": {
    "gjelderId": "12345678900",
    "gjelderNavn": "Foo Bar",
    "datoBeregnet": "2019-09-05",
    "totalBelop": 29000.00,
    "periodeList": [
      {
        "fom": "2019-01-01",
        "tom": "2019-01-12",
        "utbetaling": [
          {
            "fagSystemId": "strMdKfKSG2wOlXDS-yJNg",
            "utbetalesTilId": "00123456789",
            "utbetalesTilNavn": "Foo Bar Firma",
            "forfall": "2019-09-05",
            "feilkonto": false,
            "detaljer": [
              {
                "faktiskFom": "2019-01-01",
                "faktiskTom": "2019-01-12",
                "konto": "2338020",
                "belop": 5400.00,
                "tilbakeforing": false,
                "sats": 600.00,
                "typeSats": "DAGLIG",
                "antallSats": 9.00,
                "uforegrad": 50,
                "klassekode": "SPREFAG-IOP",
                "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                "utbetalingsType": "YTELSE",
                "refunderesOrgNr": "00123456788"
              }
            ]
          }
        ]
      },
      {
        "fom": "2019-02-13",
        "tom": "2019-02-20",
        "utbetaling": [
          {
            "fagSystemId": "strMdKfKSG2wOlXDS-yJNg",
            "utbetalesTilId": "00123456789",
            "utbetalesTilNavn": "BERGEN KOMMUNE BYRÅDSAVD",
            "forfall": "2019-09-05",
            "feilkonto": false,
            "detaljer": [
              {
                "faktiskFom": "2019-02-13",
                "faktiskTom": "2019-02-20",
                "konto": "2338020",
                "belop": 3600.00,
                "tilbakeforing": false,
                "sats": 600.00,
                "typeSats": "DAGLIG",
                "antallSats": 6.00,
                "uforegrad": 70,
                "klassekode": "SPREFAG-IOP",
                "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                "utbetalingsType": "YTELSE",
                "refunderesOrgNr": "00123456788"
              }
            ]
          }
        ]
      },
      {
        "fom": "2019-03-18",
        "tom": "2019-03-31",
        "utbetaling": [
          {
            "fagSystemId": "strMdKfKSG2wOlXDS-yJNg",
            "utbetalesTilId": "00123456789",
            "utbetalesTilNavn": "BERGEN KOMMUNE BYRÅDSAVD",
            "forfall": "2019-09-05",
            "feilkonto": false,
            "detaljer": [
              {
                "faktiskFom": "2019-03-18",
                "faktiskTom": "2019-03-31",
                "konto": "2338020",
                "belop": 10000.00,
                "tilbakeforing": false,
                "sats": 1000.00,
                "typeSats": "DAGLIG",
                "antallSats": 10.00,
                "uforegrad": 100,
                "klassekode": "SPREFAG-IOP",
                "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                "utbetalingsType": "YTELSE",
                "refunderesOrgNr": "00123456788"
              }
            ]
          }
        ]
      },
      {
        "fom": "2019-04-01",
        "tom": "2019-04-12",
        "utbetaling": [
          {
            "fagSystemId": "strMdKfKSG2wOlXDS-yJNg",
            "utbetalesTilId": "00123456789",
            "utbetalesTilNavn": "BERGEN KOMMUNE BYRÅDSAVD",
            "forfall": "2019-09-05",
            "feilkonto": false,
            "detaljer": [
              {
                "faktiskFom": "2019-04-01",
                "faktiskTom": "2019-04-12",
                "konto": "2338020",
                "belop": 10000.00,
                "tilbakeforing": false,
                "sats": 1000.00,
                "typeSats": "DAGLIG",
                "antallSats": 10.00,
                "uforegrad": 100,
                "klassekode": "SPREFAG-IOP",
                "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                "utbetalingsType": "YTELSE",
                "refunderesOrgNr": "00123456788"
              }
            ]
          }
        ]
      }
    ]
  }
}
"""