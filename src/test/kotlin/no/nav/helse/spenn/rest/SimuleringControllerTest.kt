package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.*
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.simulering.Simulering
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.security.oidc.test.support.JwtTokenGenerator
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(properties = [
    "no.nav.security.oidc.issuer.ourissuer.accepted_audience=aud-localhost",
    "no.nav.security.oidc.issuer.ourissuer.discoveryurl=http://localhost:33333/.well-known/openid-configuration",
    "api.access.requiredgroup=$requiredGroupMembership"])
@Import(AuditSupport::class)
@MockBean(HealthStatusController::class, KafkaStreams::class, OppdragStateService::class)
class SimuleringControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var aktørTilFnrMapper: AktørTilFnrMapper

    @MockBean
    lateinit var simuleringService: SimuleringService

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

    @Test
    fun runSimulering() {
        val vedtak = defaultObjectMapper.readValue(etVedtakJson,Vedtak::class.java)
        val oppdragStateDTO = OppdragStateDTO(utbetalingsOppdrag = vedtak.tilUtbetaling("12345"), soknadId = vedtak.soknadId,
                simuleringResult = SimuleringResult(status= Status.OK, simulering = Simulering(gjelderId = "12345678900",
                        gjelderNavn = "Foo Bar", datoBeregnet = LocalDate.now(),
                        totalBelop = BigDecimal.valueOf(1000), periodeList = emptyList())))
        given(aktørTilFnrMapper.tilFnr("12345")).willReturn("12345678900")
        given(simuleringService.runSimulering(any())).willReturn(oppdragStateDTO)
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf(requiredGroupMembership)))
        val requestBuilder = MockMvcRequestBuilders
                .post("/api/v1/simulering")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(etVedtakJson)
        val body = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
        println(body)
    }
}

val etVedtakJson = """{"soknadId":"9ada4305-1e45-4d48-ba48-a504bc96040d","aktorId":"12345","vedtaksperioder":[{"fom":"2020-01-15","tom":"2020-01-30","grad":100,"dagsats":1234,"fordeling":[{"mottager":"897654321","andel":100}]}],"maksDato":"2020-09-03"}"""