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