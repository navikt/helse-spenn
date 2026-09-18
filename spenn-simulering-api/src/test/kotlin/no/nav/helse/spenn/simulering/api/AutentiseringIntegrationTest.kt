package no.nav.helse.spenn.simulering.api

import no.nav.sykepenger.libs.testing.assertions.assertJsonEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AutentiseringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `avviser kall uten token`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(bearerToken = null)

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(401, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:unauthorized",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Unauthorized",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser token med feil audience`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(bearerToken = bearerToken(audience = "en-annen-app"))

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(401, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:unauthorized",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Unauthorized",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser token uten azp_name i claims`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(bearerToken = bearerToken(claims = emptyMap()))

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(401, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:unauthorized",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Unauthorized",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }
}
