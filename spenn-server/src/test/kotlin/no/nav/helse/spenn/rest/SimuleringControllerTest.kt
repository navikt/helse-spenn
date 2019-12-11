package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class SimuleringControllerTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider(server)
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }
    }

    val apienv = mockApiEnvironment()

    /*@Test
    fun runSimulering() {
        val vedtak = defaultObjectMapper.readValue(etVedtakJson,Vedtak::class.java)
        val TransaksjonDTO = TransaksjonDTO(utbetalingsOppdrag = vedtak.tilUtbetaling("12345"), soknadId = vedtak.soknadId,
                simuleringResult = SimuleringResult(status= Status.OK, simulering = Simulering(gjelderId = "12345678900",
                        gjelderNavn = "Foo Bar", datoBeregnet = LocalDate.now(),
                        totalBelop = BigDecimal.valueOf(1000), periodeList = emptyList())))
        kWhen(apienv.aktørTilFnrMapper.tilFnr("12345")).thenReturn("12345678900")
        kWhen(apienv.simuleringService.runSimulering(any())).thenReturn(TransaksjonDTO)

        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser",
                groups = listOf(apienv.authConfig.requiredGroup),
                navIdent = "X121212"))

        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/simulering") {
                setBody(etVedtakJson)
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                println(response.content)
            }
        }
    }*/
}

val etVedtakJson = """{"soknadId":"9ada4305-1e45-4d48-ba48-a504bc96040d","aktorId":"12345","vedtaksperioder":[{"fom":"2020-01-15","tom":"2020-01-30","grad":100,"dagsats":1234,"fordeling":[{"mottager":"897654321","andel":100}]}],"maksDato":"2020-09-03"}"""