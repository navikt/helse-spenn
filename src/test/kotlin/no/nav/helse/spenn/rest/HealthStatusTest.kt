package no.nav.helse.spenn.rest

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class HealthStatusTest {

    val apienv = SpennApiEnvironment(
            kafkaStreams = mock(KafkaStreams::class.java),
            meterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())
    )

    @Test
    fun isALiveShouldReturn_200_ALIVE_whenEverythingOK() {
        mockKafkaIsRunning(true)
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
            mockKafkaIsRunning(false)
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
            mockKafkaIsRunning(true)
            handleRequest(HttpMethod.Get, "internal/isAlive").apply {
                assertEquals(HttpStatusCode.OK, response.status(), "running=true should reset to OK")
                assertEquals("ALIVE", response.content)
            }
        }

    }

    @Test
    fun isReadyShouldReturn_200_READY_whenEverythingOK() {
        mockKafkaIsRunning(true)
        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Get, "internal/isReady").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("READY", response.content)
            }
        }
    }

    private fun mockKafkaIsRunning(isRunning: Boolean = true) {
        Mockito.reset(apienv.kafkaStreams)
        val mockState = mock(KafkaStreams.State::class.java)
        Mockito.`when`(mockState.isRunning).thenReturn(isRunning)
        Mockito.`when`(apienv.kafkaStreams.state()).thenReturn(mockState)
    }
}