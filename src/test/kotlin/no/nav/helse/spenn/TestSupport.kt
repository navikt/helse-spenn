package no.nav.helse.spenn

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.JWTClaimsSet
import no.nav.helse.spenn.vedtak.Fordeling
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.Vedtaksperiode
import no.nav.security.oidc.test.support.JwkGenerator
import no.nav.security.oidc.test.support.JwtTokenGenerator
import org.mockito.Mockito
import java.time.LocalDate
import java.util.*

const val requiredGroupMembership = "12345678-abcd-abcd-eeff-1234567890ab"

fun buildClaimSet(subject: String,
                  issuer: String = JwtTokenGenerator.ISS,
                  audience: String = JwtTokenGenerator.AUD,
                  authLevel: String = JwtTokenGenerator.ACR,
                  expiry: Long = JwtTokenGenerator.EXPIRY,
                  issuedAt: Date = Date(),
                  navIdent: String? = null,
                  groups: List<String>? = null): JWTClaimsSet {
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
    if (groups != null) {
        builder.claim("groups", groups)
    }
    return builder.build()
}

fun stubOIDCProvider(server: WireMockServer) {
    WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
            WireMock.okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                    "\"subject_types_supported\": [\"pairwise\"], " +
                    "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

    WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
            WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
}

fun etEnkeltVedtak(): Vedtak {
    return Vedtak(
            soknadId = UUID.randomUUID(),
            aktorId = "en random aktørid",
            vedtaksperioder = listOf(Vedtaksperiode(
                    fom = LocalDate.of(2020, 1, 15),
                    tom = LocalDate.of(2020, 1, 30),
                    dagsats = 1234,
                    fordeling = listOf(Fordeling(
                            mottager = "897654321",
                            andel = 100
                    ))
            )),
            maksDato = LocalDate.now().plusYears(1)
    )
}

fun <T> any(): T = Mockito.any<T>()

fun <T> kArgThat(matcher: (T) -> Boolean): T = Mockito.argThat<T>(matcher)