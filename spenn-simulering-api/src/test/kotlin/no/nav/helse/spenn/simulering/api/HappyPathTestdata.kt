package no.nav.helse.spenn.simulering.api

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import org.intellij.lang.annotations.Language

enum class HappyPathTestdata(
    @Language("JSON")
    val request: String,
    val forventetRequestTilSimuleringServiceFactory: (samlToken: String) -> String,
    val simuleringServiceSvar: ResponseDefinitionBuilder,
    @Language("JSON")
    val forventetResponse: String,
) {
    ARBEIDSGIVERREFUSJON(
        request =
            """
            {
              "fødselsnummer": "12345678911",
              "maksdato": "2018-12-31",
              "saksbehandler": "SPENN",
              "oppdrag": {
                "fagområde": "ARBEIDSGIVERREFUSJON",
                "fagsystemId": "a1b0c2",
                "endringskode": "NY",
                "mottakerAvUtbetalingen": "123456789",
                "linjer": [
                  {
                    "endringskode": "NY",
                    "fom": "2018-01-01",
                    "tom": "2018-01-31",
                    "satstype": "DAGLIG",
                    "sats": 1000,
                    "grad": 100,
                    "delytelseId": 1,
                    "refDelytelseId": null,
                    "refFagsystemId": null,
                    "klassekode": "REFUSJON_IKKE_OPPLYSNINGSPLIKTIG",
                    "klassekodeFom": "2018-01-01",
                    "opphørerFom": null
                  }
                ]
              }
            }
            """.trimIndent(),
        forventetRequestTilSimuleringServiceFactory =
            { samlToken ->
                // language=xml
                """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Header>
                        <Action xmlns="http://www.w3.org/2005/08/addressing">http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt/simulerFpService/simulerBeregningRequest</Action>
                        <MessageID xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:{{messageId}}</MessageID>
                        <To xmlns="http://www.w3.org/2005/08/addressing">{{serviceUrl}}</To>
                        <ReplyTo xmlns="http://www.w3.org/2005/08/addressing">
                            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
                        </ReplyTo>
                        <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                            soap:mustUnderstand="1">
                            $samlToken
                        </wsse:Security>
                    </soap:Header>
                    <soap:Body>
                        <ns2:simulerBeregningRequest xmlns:ns2="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt"
                            xmlns:ns3="http://nav.no/system/os/entiteter/oppdragSkjema">
                            <request>
                                <simuleringsPeriode>
                                    <datoSimulerFom>2018-01-01</datoSimulerFom>
                                    <datoSimulerTom>2018-01-31</datoSimulerTom>
                                </simuleringsPeriode>
                                <oppdrag>
                                    <kodeEndring>NY</kodeEndring>
                                    <kodeFagomraade>SPREF</kodeFagomraade>
                                    <fagsystemId>a1b0c2</fagsystemId>
                                    <utbetFrekvens>MND</utbetFrekvens>
                                    <oppdragGjelderId>12345678911</oppdragGjelderId>
                                    <datoOppdragGjelderFom>1970-01-01</datoOppdragGjelderFom>
                                    <saksbehId>SPENN</saksbehId>
                                    <ns3:enhet>
                                        <typeEnhet>BOS</typeEnhet>
                                        <enhet>8020</enhet>
                                        <datoEnhetFom>1970-01-01</datoEnhetFom>
                                    </ns3:enhet>
                                    <oppdragslinje>
                                        <kodeEndringLinje>NY</kodeEndringLinje>
                                        <delytelseId>1</delytelseId>
                                        <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
                                        <datoVedtakFom>2018-01-01</datoVedtakFom>
                                        <datoVedtakTom>2018-01-31</datoVedtakTom>
                                        <sats>1000</sats>
                                        <fradragTillegg>T</fradragTillegg>
                                        <typeSats>DAG</typeSats>
                                        <brukKjoreplan>N</brukKjoreplan>
                                        <saksbehId>SPENN</saksbehId>
                                        <ns3:grad>
                                            <typeGrad>UFOR</typeGrad>
                                            <grad>100</grad>
                                        </ns3:grad>
                                        <ns3:attestant>
                                            <attestantId>SPENN</attestantId>
                                        </ns3:attestant>
                                        <ns3:refusjonsInfo>
                                            <refunderesId>00123456789</refunderesId>
                                            <maksDato>2018-12-31</maksDato>
                                            <datoFom>2018-01-01</datoFom>
                                        </ns3:refusjonsInfo>
                                    </oppdragslinje>
                                </oppdrag>
                            </request>
                        </ns2:simulerBeregningRequest>
                    </soap:Body>
                </soap:Envelope>
                """.trimIndent()
            },
        simuleringServiceSvar =
            WireMock.okTextXml(
                // language=xml
                """
                <?xml version='1.0' encoding='UTF-8'?>
                <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
                    <S:Body>
                        <simulerBeregningResponse xmlns="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt">
                            <response xmlns="">
                                <simulering>
                                    <gjelderId>12345678911</gjelderId>
                                    <gjelderNavn>NORMAL MUFFINS</gjelderNavn>
                                    <datoBeregnet>2018-02-01</datoBeregnet>
                                    <kodeFaggruppe>KORTTID</kodeFaggruppe>
                                    <belop>15000</belop>
                                    <beregningsPeriode>
                                        <periodeFom>2018-01-01</periodeFom>
                                        <periodeTom>2018-01-31</periodeTom>
                                        <beregningStoppnivaa>
                                            <kodeFagomraade>SPREF</kodeFagomraade>
                                            <stoppNivaaId>1</stoppNivaaId>
                                            <behandlendeEnhet>8020</behandlendeEnhet>
                                            <oppdragsId>123</oppdragsId>
                                            <fagsystemId>a1b0c2</fagsystemId>
                                            <utbetalesTilId>00123456789</utbetalesTilId>
                                            <utbetalesTilNavn>ARBEIDSGIVER AS</utbetalesTilNavn>
                                            <bilagsType>U</bilagsType>
                                            <forfall>2018-02-01</forfall>
                                            <feilkonto>false</feilkonto>
                                            <beregningStoppnivaaDetaljer>
                                                <faktiskFom>2018-01-01</faktiskFom>
                                                <faktiskTom>2018-01-31</faktiskTom>
                                                <kontoStreng>1234567890</kontoStreng>
                                                <behandlingskode>2</behandlingskode>
                                                <belop>15000</belop>
                                                <trekkVedtakId>0</trekkVedtakId>
                                                <tilbakeforing>false</tilbakeforing>
                                                <linjeId>1</linjeId>
                                                <sats>1000.00</sats>
                                                <typeSats>DAG</typeSats>
                                                <antallSats>15</antallSats>
                                                <saksbehId>SPENN</saksbehId>
                                                <uforeGrad>100</uforeGrad>
                                                <klassekode>SPREFAG-IOP</klassekode>
                                                <klasseKodeBeskrivelse>Sykepenger, Refusjon arbeidsgiver</klasseKodeBeskrivelse>
                                                <typeKlasse>YTEL</typeKlasse>
                                                <refunderesOrgNr>00123456789</refunderesOrgNr>
                                            </beregningStoppnivaaDetaljer>
                                        </beregningStoppnivaa>
                                    </beregningsPeriode>
                                </simulering>
                            </response>
                        </simulerBeregningResponse>
                    </S:Body>
                </S:Envelope>
                """.trimIndent(),
            ),
        forventetResponse =
            """
            {
              "gjelderId": "12345678911",
              "gjelderNavn": "NORMAL MUFFINS",
              "datoBeregnet": "2018-02-01",
              "totalBelop": 15000,
              "periodeList": [
                {
                  "fom": "2018-01-01",
                  "tom": "2018-01-31",
                  "utbetaling": [
                    {
                      "fagSystemId": "a1b0c2",
                      "utbetalesTilId": "123456789",
                      "utbetalesTilNavn": "ARBEIDSGIVER AS",
                      "forfall": "2018-02-01",
                      "feilkonto": false,
                      "detaljer": [
                        {
                          "faktiskFom": "2018-01-01",
                          "faktiskTom": "2018-01-31",
                          "konto": "1234567890",
                          "belop": 15000,
                          "tilbakeforing": false,
                          "sats": 1000.0,
                          "typeSats": "DAG",
                          "antallSats": 15,
                          "uforegrad": 100,
                          "klassekode": "SPREFAG-IOP",
                          "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                          "utbetalingsType": "YTEL",
                          "refunderesOrgNr": "123456789"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
    ),

    BRUKERUTBETALING(
        request =
            """
            {
              "fødselsnummer": "12345678911",
              "maksdato": null,
              "saksbehandler": "SPENN",
              "oppdrag": {
                "fagområde": "BRUKERUTBETALING",
                "fagsystemId": "a1b0c2",
                "endringskode": "ENDRET",
                "mottakerAvUtbetalingen": "12345678911",
                "linjer": [
                  {
                    "endringskode": "ENDRET",
                    "fom": "2018-01-01",
                    "tom": "2018-01-31",
                    "satstype": "ENGANGS",
                    "sats": 15000,
                    "grad": null,
                    "delytelseId": 2,
                    "refDelytelseId": 1,
                    "refFagsystemId": "a1b0c2",
                    "klassekode": "SYKEPENGER_ARBEIDSTAKER_ORDINÆR",
                    "klassekodeFom": null,
                    "opphørerFom": "2018-01-15"
                  }
                ]
              }
            }
            """.trimIndent(),
        forventetRequestTilSimuleringServiceFactory =
            { samlToken ->
                // language=xml
                """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Header>
                        <Action xmlns="http://www.w3.org/2005/08/addressing">http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt/simulerFpService/simulerBeregningRequest</Action>
                        <MessageID xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:{{messageId}}</MessageID>
                        <To xmlns="http://www.w3.org/2005/08/addressing">{{serviceUrl}}</To>
                        <ReplyTo xmlns="http://www.w3.org/2005/08/addressing">
                            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
                        </ReplyTo>
                        <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                            soap:mustUnderstand="1">
                            $samlToken
                        </wsse:Security>
                    </soap:Header>
                    <soap:Body>
                        <ns2:simulerBeregningRequest xmlns:ns2="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt"
                            xmlns:ns3="http://nav.no/system/os/entiteter/oppdragSkjema">
                            <request>
                                <simuleringsPeriode>
                                    <datoSimulerFom>2018-01-01</datoSimulerFom>
                                    <datoSimulerTom>2018-01-31</datoSimulerTom>
                                </simuleringsPeriode>
                                <oppdrag>
                                    <kodeEndring>ENDR</kodeEndring>
                                    <kodeFagomraade>SP</kodeFagomraade>
                                    <fagsystemId>a1b0c2</fagsystemId>
                                    <utbetFrekvens>MND</utbetFrekvens>
                                    <oppdragGjelderId>12345678911</oppdragGjelderId>
                                    <datoOppdragGjelderFom>1970-01-01</datoOppdragGjelderFom>
                                    <saksbehId>SPENN</saksbehId>
                                    <ns3:enhet>
                                        <typeEnhet>BOS</typeEnhet>
                                        <enhet>8020</enhet>
                                        <datoEnhetFom>1970-01-01</datoEnhetFom>
                                    </ns3:enhet>
                                    <oppdragslinje>
                                        <kodeEndringLinje>ENDR</kodeEndringLinje>
                                        <delytelseId>2</delytelseId>
                                        <refDelytelseId>1</refDelytelseId>
                                        <refFagsystemId>a1b0c2</refFagsystemId>
                                        <kodeKlassifik>SPATORD</kodeKlassifik>
                                        <kodeStatusLinje>OPPH</kodeStatusLinje>
                                        <datoStatusFom>2018-01-15</datoStatusFom>
                                        <datoVedtakFom>2018-01-01</datoVedtakFom>
                                        <datoVedtakTom>2018-01-31</datoVedtakTom>
                                        <sats>15000</sats>
                                        <fradragTillegg>T</fradragTillegg>
                                        <typeSats>ENG</typeSats>
                                        <brukKjoreplan>N</brukKjoreplan>
                                        <saksbehId>SPENN</saksbehId>
                                        <utbetalesTilId>12345678911</utbetalesTilId>
                                        <ns3:attestant>
                                            <attestantId>SPENN</attestantId>
                                        </ns3:attestant>
                                    </oppdragslinje>
                                </oppdrag>
                            </request>
                        </ns2:simulerBeregningRequest>
                    </soap:Body>
                </soap:Envelope>
                """.trimIndent()
            },
        simuleringServiceSvar =
            WireMock.okTextXml(
                // language=xml
                """
                <?xml version='1.0' encoding='UTF-8'?>
                <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
                    <S:Body>
                        <simulerBeregningResponse xmlns="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt">
                            <response xmlns="">
                                <simulering>
                                    <gjelderId>12345678911</gjelderId>
                                    <gjelderNavn>NORMAL MUFFINS</gjelderNavn>
                                    <datoBeregnet>2018-02-01</datoBeregnet>
                                    <kodeFaggruppe>KORTTID</kodeFaggruppe>
                                    <belop>0</belop>
                                </simulering>
                            </response>
                        </simulerBeregningResponse>
                    </S:Body>
                </S:Envelope>
                """.trimIndent(),
            ),
        forventetResponse =
            """
            {
              "gjelderId": "12345678911",
              "gjelderNavn": "NORMAL MUFFINS",
              "datoBeregnet": "2018-02-01",
              "totalBelop": 0,
              "periodeList": []
            }
            """.trimIndent(),
    ),
}
