package no.nav.helse.spenn.sts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StsRestClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        configureFor(server.port())
    }

    @Test
    fun `should parse a token successfully`() {
        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("default")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        val token: String = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar").token()
        assertEquals("default access token", token)
    }

    @Test
    fun `should cache tokens`() {
        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("caching")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        stubFor(stsRequestMapping
                .willReturn(ok(bad_token))
                .inScenario("caching")
                .whenScenarioStateIs("token acquired")
        )

        val authHelper = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")
        val first = authHelper.token()

        val second: String = authHelper.token()

        assertEquals(first, second)
        assertEquals("default access token", second)
    }

    @Test
    fun `should get new token when old has expired`() {
        stubFor(stsRequestMapping
                .willReturn(ok(short_lived_token))
                .inScenario("expiry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("expired token sent"))

        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("expiry")
                .whenScenarioStateIs("expired token sent")
        )

        val client = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")

        // get the short-lived one
        val token1 = client.token()

        // get the new one
        val token2 = client.token()

        assertNotEquals(token1, token2)
        assertEquals("default access token", token2)
    }

}

private val stsRequestMapping: MappingBuilder = get(urlPathEqualTo("/rest/v1/sts/token"))
        .withQueryParam("grant_type", equalTo("client_credentials"))
        .withQueryParam("scope", equalTo("openid"))
        .withBasicAuth("foo", "bar")
        .withHeader("Accept", equalTo("application/json"))

private val default_token = """{
  "access_token": "default access token",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()

private val short_lived_token = """{
  "access_token": "short lived token",
  "token_type": "Bearer",
  "expires_in": 1
}""".trimIndent()

private val bad_token = """{
  "access_token": "this token shouldn't be requested",
  "token_type": "Bearer",
  "expires_in": 1000000000000
}""".trimIndent()

