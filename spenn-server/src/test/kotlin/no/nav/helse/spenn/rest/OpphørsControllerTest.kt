package no.nav.helse.spenn.rest

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
import no.nav.helse.spenn.oppdrag.Utbetalingsbehov
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.buildClaimSet
import no.nav.helse.spenn.enEnkelAnnulering
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.mockApiEnvironment
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.stubOIDCProvider
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.toOppdragsbehov
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class OpphørsControllerTest {

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

    @Test
    fun runOpphør() {
        val apienv = mockApiEnvironment().copy(
                stateService = OppdragService(TestDb.createMigratedDataSource())
        )
        val behov = enEnkelAnnulering()

        apienv.stateService.lagreNyttOppdrag(Utbetalingsbehov(etEnkeltBehov(), "12345678900"))
        apienv.stateService.lagreNyttOppdrag(lagOppdragFraBehov(etEnkeltBehov().toOppdragsbehov()))

        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser",
                groups = listOf(apienv.authConfig.requiredGroup),
                preferredUsername = "sara saksbehandler"))

        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/opphor") {
                setBody(behov.toString())
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
        }
    }

    @Test
    fun runDobbeltOpphør() {
        val apienv = mockApiEnvironment().copy(
                stateService = OppdragService(TestDb.createMigratedDataSource())
        )
        val behov = enEnkelAnnulering()

        apienv.stateService.lagreNyttOppdrag(Utbetalingsbehov(etEnkeltBehov(), "12345678900"))
        apienv.stateService.lagreNyttOppdrag(lagOppdragFraBehov(etEnkeltBehov().toOppdragsbehov()))

        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser",
                groups = listOf(apienv.authConfig.requiredGroup),
                preferredUsername = "sara saksbehandler"))

        withTestApplication({
            spennApiModule(apienv)
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/opphor") {
                setBody(behov.toString())
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            handleRequest(HttpMethod.Post, "/api/v1/opphor") {
                setBody(behov.toString())
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }
}
