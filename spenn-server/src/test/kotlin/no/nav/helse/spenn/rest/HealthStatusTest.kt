package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.mockApiEnvironment
import no.nav.helse.spenn.stubOIDCProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class HealthStatusTest {

    val apienv = mockApiEnvironment()

    @Test
    fun isALiveShouldReturn_200_ALIVE_whenEverythingOK() {
        apienv.mockKafkaIsRunning(true)
        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Get, "internal/isAlive").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("ALIVE", response.content)
            }
        }
    }

    @Test
    fun isAliveShouldReturnFAILED_DEPENDENCY_whenKafkaNotRunningAfter61rounds_butResetTo_OK_whenOkAgain() {
        withTestApplication({
            spennApiModule(apienv)
        }) {
            apienv.mockKafkaIsRunning(false)
            handleRequest(HttpMethod.Get, "internal/isAlive").apply {
                assertEquals(HttpStatusCode.OK, response.status(), "running=false one time should be OK")
                assertEquals("ALIVE", response.content)
            }
            for (i in 1..60) {
                handleRequest(HttpMethod.Get, "internal/isAlive")
            }
            handleRequest(HttpMethod.Get, "internal/isAlive").apply {
                assertEquals(HttpStatusCode.FailedDependency, response.status(), "running=false one time should be OK")
            }
            apienv.mockKafkaIsRunning(true)
            handleRequest(HttpMethod.Get, "internal/isAlive").apply {
                assertEquals(HttpStatusCode.OK, response.status(), "running=true should reset to OK")
                assertEquals("ALIVE", response.content)
            }
        }

    }

    @Test
    fun isReadyShouldReturn_200_READY_whenEverythingOK() {
        apienv.mockKafkaIsRunning(true)
        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Get, "internal/isReady").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("READY", response.content)
            }
        }
    }

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
}