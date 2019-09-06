package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.buildClaimSet
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
import no.nav.helse.spenn.requiredGroupMembership
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.stubOIDCProvider
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.security.oidc.test.support.JwtTokenGenerator
import org.apache.kafka.streams.KafkaStreams
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month
import java.util.*

@WebMvcTest(properties = [
    "no.nav.security.oidc.issuer.ourissuer.accepted_audience=aud-localhost",
    "no.nav.security.oidc.issuer.ourissuer.discoveryurl=http://localhost:33333/.well-known/openid-configuration",
    "api.access.requiredgroup=$requiredGroupMembership"])
@Import(AuditSupport::class)
@MockBean(HealthStatusController::class, SimuleringService::class, KafkaStreams::class, AktørTilFnrMapper::class)
class RekjoringControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var oppdragStateService: OppdragStateService

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

        val fom = LocalDate.of(2019, Month.APRIL, 2)
        val tom = LocalDate.of(2019, Month.APRIL, 16)
        val linje = UtbetalingsLinje(id = "1234567890", datoFom = fom,
                datoTom = tom, sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))
        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "995816598", utbetalingsLinje = listOf(linje),
                vedtak = Vedtak(
                        soknadId = UUID.randomUUID(),
                        maksDato = LocalDate.now().plusYears(1),
                        aktorId = "12341234",
                        vedtaksperioder = emptyList()
                ))
        val feil = OppdragStateDTO(id = 1L, soknadId = UUID.randomUUID(),
                utbetalingsOppdrag = utbetaling, status = OppdragStateStatus.FEIL)
        val simulerFeil = feil.copy(id=2L, status=OppdragStateStatus.SIMULERING_FEIL, soknadId = UUID.randomUUID())
        val ikkeFeil = feil.copy(id=3L, soknadId = UUID.randomUUID(), status = OppdragStateStatus.FERDIG)
        val feil2 = feil.copy(4L, soknadId = UUID.randomUUID())
    }

    @Test
    fun runresetStateTest() {
        given(oppdragStateService.fetchOppdragState(ikkeFeil.soknadId)).willReturn(ikkeFeil)
        given(oppdragStateService.fetchOppdragState(feil.soknadId)).willReturn(feil)
        given(oppdragStateService.fetchOppdragState(simulerFeil.soknadId)).willReturn(simulerFeil)
        given(oppdragStateService.fetchOppdragState(feil2.soknadId)).willReturn(feil2)
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf(requiredGroupMembership)))
        val requestBuilder = MockMvcRequestBuilders
                .put("/api/v1/rekjoring?fagId=${feil.soknadId.toFagId()},${simulerFeil.soknadId.toFagId()}&soknadId=${ikkeFeil.soknadId},${feil2.soknadId}")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()", `is`(3)))
    }

    @Test
    fun runResetStateForAllTest() {
        given(oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.FEIL)).willReturn(listOf(feil, feil2))
        given(oppdragStateService.fetchOppdragStateByStatus(OppdragStateStatus.SIMULERING_FEIL)).willReturn(listOf(simulerFeil))
        val jwt = JwtTokenGenerator.createSignedJWT(buildClaimSet(subject = "testuser", groups = listOf(requiredGroupMembership)))
        val allRequest = MockMvcRequestBuilders
                .put("/api/v1/rekjoring/all")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")
        mockMvc.perform(allRequest)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()", `is`(2)))
        val allIncludeSimulering = MockMvcRequestBuilders
                .put("/api/v1/rekjoring/all?includeSimulering=true")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${jwt.serialize()}")
        mockMvc.perform(allIncludeSimulering)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()", `is`(3)))


    }
}