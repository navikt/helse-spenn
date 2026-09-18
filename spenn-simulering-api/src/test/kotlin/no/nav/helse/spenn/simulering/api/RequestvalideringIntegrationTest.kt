package no.nav.helse.spenn.simulering.api

import no.nav.sykepenger.libs.testing.assertions.assertJsonEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RequestvalideringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `avviser kall uten brukernavn for serviceuser`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(serviceUserUsername = null)

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(500, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: Missing header X-ServiceUser-Username",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser kall uten passord for serviceuser`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(serviceUserPassword = null)

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(500, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: Missing header X-ServiceUser-Password",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser kall med blankt brukernavn for serviceuser`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(serviceUserUsername = "  ")

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(500, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: Missing header X-ServiceUser-Username",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser kall med blankt passord for serviceuser`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(serviceUserPassword = "  ")

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(500, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:internal_error",
                  "title": "Internal Server Error",
                  "status": 500,
                  "detail": "Uventet feil: Missing header X-ServiceUser-Password",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser oppdrag uten linjer`() {
        val (responseStatus, responseBody) =
            settOppOgUtførPostSimulering(
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
                        "linjer": []
                      }
                    }
                    """.trimIndent(),
            )

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(400, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:bad_request",
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Ugyldig simulering request, nytteløst å simulere oppdrag uten linjer",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser request som mangler påkrevde felter`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(request = """{ "fødselsnummer": "12345678911" }""")

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(400, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:bad_request",
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Failed to convert request body to class no.nav.helse.spenn.simulering.api.SimuleringRequest",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser request med ukjent verdi i enum`() {
        val (responseStatus, responseBody) =
            settOppOgUtførPostSimulering(
                request =
                    HappyPathTestdata.ARBEIDSGIVERREFUSJON.request
                        .replace("ARBEIDSGIVERREFUSJON", "ET_UKJENT_FAGOMRÅDE"),
            )

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(400, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:bad_request",
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Failed to convert request body to class no.nav.helse.spenn.simulering.api.SimuleringRequest",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }

    @Test
    fun `avviser request som ikke er gyldig JSON`() {
        val (responseStatus, responseBody) = settOppOgUtførPostSimulering(request = "dette er ikke JSON")

        forventIngenRequestsTilGandalf()
        forventIngenRequestsTilSimuleringService()

        assertEquals(400, responseStatus)
        assertJsonEquals(
            expectedJson =
                """
                {
                  "type": "urn:error:bad_request",
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Failed to convert request body to class no.nav.helse.spenn.simulering.api.SimuleringRequest",
                  "instance": "/api/simulering"
                }
                """.trimIndent(),
            actualJson = responseBody,
            bortsettFraStier = setOf("callId", "stacktrace"),
        )
    }
}
