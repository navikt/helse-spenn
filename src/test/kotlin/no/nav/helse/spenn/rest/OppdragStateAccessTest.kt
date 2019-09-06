package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.buildClaimSet
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.requiredGroupMembership
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.stubOIDCProvider
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.security.oidc.test.support.JwtTokenGenerator
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import kotlin.test.assertEquals

// I prod/Q, sett med env-variabler slik:
// NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE=aud-localhost
// NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL=http://localhost:33333/.well-known/openid-configuration
// API_ACCESS_REQUIREDGROUP=12345678-abcd-abcd-eeff-1234567890ab
// og eventuelt: NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_PROXY_URL=http://someproxy:8080

@WebMvcTest(properties = [
    "no.nav.security.oidc.issuer.ourissuer.accepted_audience=aud-localhost",
    "no.nav.security.oidc.issuer.ourissuer.discoveryurl=http://localhost:33333/.well-known/openid-configuration",
    "api.access.requiredgroup=$requiredGroupMembership"])
@Import(AuditSupport::class)
@MockBean(HealthStatusController::class, SimuleringService::class, KafkaStreams::class,
        AktørTilFnrMapper::class, OppdragStateService::class)
class OppdragStateAccessTest {

    @Autowired
    lateinit var mockMvc: MockMvc


    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            configureFor(server.port())
            stubOIDCProvider(server)
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
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
    fun missingGroupsClaimInJWTshouldGive401() {
        val jwt = JwtTokenGenerator.createSignedJWT("testuser")
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(401, result.response.status)
    }

    @Test
    fun notHavingTheRightGroupInJWTshouldGive401() {
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf("someBadGroupOID")))
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(401, result.response.status)
    }

    @Test
    fun notHavingTheRightGroupInJWT_whenGettingByOppdragId_shouldGive401() {
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf("someBadGroupOID")))
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/1")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(401, result.response.status)
    }


    @Test
    fun unknownIssuerInJWTshouldGive401_evenWithOkGroupmembership() {
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", issuer = "someUnknownISsuer", groups = listOf(requiredGroupMembership)))
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(401, result.response.status)
    }

    @Test
    fun correctGroupMembershipInJWTshouldGive200() {
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf(requiredGroupMembership)))
        val requestBuilder = MockMvcRequestBuilders
                .get("/api/v1/oppdrag/soknad/5a18a938-b747-4ab2-bb35-5d338dea15c8")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")

        val result = mockMvc.perform(requestBuilder).andReturn()
        assertEquals(200, result.response.status)
    }



}