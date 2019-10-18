package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.accept
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
import no.nav.helse.spenn.vedtak.fnr.DummyAktørMapper
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.security.token.support.test.JwtTokenGenerator
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.mock
/*import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status*/
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*@WebMvcTest(properties = [
    "no.nav.security.oidc.issuer.ourissuer.accepted_audience=aud-localhost",
    "no.nav.security.oidc.issuer.ourissuer.discoveryurl=http://localhost:33333/.well-known/openid-configuration",
    "api.access.requiredgroup=$requiredGroupMembership"])
@Import(AuditSupport::class)
@MockBean(HealthStatusController::class, KafkaStreams::class, OppdragStateService::class)*/
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

    @Test
    fun runSimulering() {
        val vedtak = defaultObjectMapper.readValue(etVedtakJson,Vedtak::class.java)
        val oppdragStateDTO = OppdragStateDTO(utbetalingsOppdrag = vedtak.tilUtbetaling("12345"), soknadId = vedtak.soknadId,
                simuleringResult = SimuleringResult(status= Status.OK, simulering = Simulering(gjelderId = "12345678900",
                        gjelderNavn = "Foo Bar", datoBeregnet = LocalDate.now(),
                        totalBelop = BigDecimal.valueOf(1000), periodeList = emptyList())))
        kWhen(apienv.aktørTilFnrMapper.tilFnr("12345")).thenReturn("12345678900")
        kWhen(apienv.simuleringService.runSimulering(any())).thenReturn(oppdragStateDTO)

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
    }
}

val etVedtakJson = """{"soknadId":"9ada4305-1e45-4d48-ba48-a504bc96040d","aktorId":"12345","vedtaksperioder":[{"fom":"2020-01-15","tom":"2020-01-30","grad":100,"dagsats":1234,"fordeling":[{"mottager":"897654321","andel":100}]}],"maksDato":"2020-09-03"}"""