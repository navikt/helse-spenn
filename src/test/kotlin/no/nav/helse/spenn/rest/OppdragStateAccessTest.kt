package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.security.oidc.test.support.JwkGenerator
import no.nav.security.oidc.test.support.JwtTokenGenerator
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import kotlin.test.assertEquals

@WebMvcTest(properties = [
    "no.nav.security.oidc.issuer.ourissuer.accepted_audience=aud-localhost",
    "no.nav.security.oidc.issuer.ourissuer.discoveryurl=http://localhost:33333/.well-known/openid-configuration"])
class OppdragStateAccessTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var kafkaStreams: KafkaStreams
    @MockBean
    lateinit var healthStatusController: HealthStatusController
    @MockBean
    lateinit var oppdragStateService: OppdragStateService

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            configureFor(server.port())
            stubOIDCProvider()
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }

        fun stubOIDCProvider() {
            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
                    WireMock.okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                            "\"subject_types_supported\": [\"pairwise\"], " +
                            "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
                    WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
        }
    }

    @Test
    fun noTokenShouldGive401() {
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(401, result.response.status)
    }

    @Test
    fun test1() {
        val jwt = JwtTokenGenerator.createSignedJWT("testuser")
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(200, result.response.status)
    }



}