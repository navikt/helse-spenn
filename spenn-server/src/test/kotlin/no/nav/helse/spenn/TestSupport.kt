package no.nav.helse.spenn

import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.core.instrument.MockClock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import java.net.URL
import java.time.LocalDate
import java.util.Date
import java.util.UUID

const val requiredGroupMembership = "12345678-abcd-abcd-eeff-1234567890ab"

fun testAuthEnv() = AuthEnvironment(
    acceptedAudience = JwtTokenGenerator.AUD,
    discoveryUrl = URL("http://localhost:33333/.well-known/openid-configuration"),
    requiredGroup = requiredGroupMembership
)

@KtorExperimentalAPI
fun mockApiEnvironment() = SpennApiEnvironment(
    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry(), MockClock()),
    authConfig = testAuthEnv(),
    simuleringService = mockk(),
    auditSupport = AuditSupport(),
    stateService = mockk()
)

fun buildClaimSet(
    subject: String,
    issuer: String = JwtTokenGenerator.ISS,
    audience: String = JwtTokenGenerator.AUD,
    authLevel: String = JwtTokenGenerator.ACR,
    expiry: Long = JwtTokenGenerator.EXPIRY,
    issuedAt: Date = Date(),
    navIdent: String? = null,
    preferredUsername: String? = null,
    groups: List<String>? = null
): JWTClaimsSet {
    val builder = JWTClaimsSet.Builder()
        .subject(subject)
        .issuer(issuer)
        .audience(audience)
        .jwtID(UUID.randomUUID().toString())
        .claim("acr", authLevel)
        .claim("ver", "1.0")
        .claim("nonce", "myNonce")
        .claim("auth_time", issuedAt)
        .notBeforeTime(issuedAt)
        .issueTime(issuedAt)
        .expirationTime(Date(issuedAt.time + expiry))
    if (navIdent != null) {
        builder.claim("NAVident", navIdent)
    }
    if (preferredUsername != null) {
        builder.claim("preferred_username", preferredUsername)
    }
    if (groups != null) {
        builder.claim("groups", groups)
    }
    return builder.build()
}

fun stubOIDCProvider(server: WireMockServer) {
    WireMock.stubFor(
        WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
            WireMock.okJson(
                "{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                        "\"subject_types_supported\": [\"pairwise\"], " +
                        "\"issuer\": \"${JwtTokenGenerator.ISS}\"}"
            )
        )
    )

    WireMock.stubFor(
        WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
            WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())
        )
    )
}

data class Behovslinje(
    val fom: String,
    val tom: String,
    val dagsats: String
)

fun utbetalingMedRef(utbetalingsreferanse: String, dagsats: Double = 1234.0) =
    lagOppdragFraBehov(
        etEnkeltBehov(
            utbetalingsreferanse = utbetalingsreferanse,
            dagsats = dagsats
        ).toOppdragsbehov()
    )

fun etEnkeltBehov(
    maksdato: LocalDate = LocalDate.now().plusYears(1),
    utbetalingsreferanse: String = "1",
    dagsats: Double = 1234.0,
    utbetalingslinje: Behovslinje = Behovslinje(
        fom = "2020-01-15",
        tom = "2020-01-30",
        dagsats = "$dagsats"
    ),
    utbetalingslinjer: List<Behovslinje> =
        listOf(utbetalingslinje)
): JsonNode = defaultObjectMapper.readTree(
    """
        {
          "@behov": "Utbetaling",
          "sakskompleksId": "e25ccad5-f5d5-4399-bb9d-43e9fc487888",
          "utbetalingsreferanse": "$utbetalingsreferanse",
          "aktørId": "1234567890123",
          "fødselsnummer": "12345678901",
          "organisasjonsnummer": "897654321",
          "maksdato": "$maksdato",
          "saksbehandler": "Z999999",
          "utbetalingslinjer": [
          ${utbetalingslinjer.joinToString(separator = ",") {
        """
                  {
                      "fom": "${it.fom}",
                      "tom": "${it.tom}",
                      "dagsats": "${it.dagsats}"
                  }
                  """
    }}
          ]
        }        
    """
)

fun enEnkelAnnulering() = defaultObjectMapper.readTree(
    """
        {
          "utbetalingsreferanse": "1",
          "aktørId": "1234567890123",
          "fødselsnummer": "12345678901",
          "saksbehandler": "Z999999"
        }        
    """
)
