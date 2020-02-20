package no.nav.helse.spenn.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.*
import no.nav.helse.spenn.testsupport.simuleringsresultat
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class SimuleringControllerTest {

    companion object {
        private val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
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

    private val apienv = mockApiEnvironment()

    @Test
    fun runSimulering() {
        val behov = etEnkeltBehov()
        (behov as ObjectNode).put("erUtvidelse", false)

        kWhen(apienv.simuleringService.simulerOppdrag(any())).thenReturn(simuleringsresultat)

        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser",
                groups = listOf(apienv.authConfig.requiredGroup),
                preferredUsername = "sara saksbehandler"))

        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/simulering") {
                setBody(behov.toString())
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
