package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.oidc.test.support.JwkGenerator
import no.nav.security.oidc.test.support.JwtTokenGenerator
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

fun stubOIDCProvider() {
    WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
            WireMock.okJson("{\"jwks_uri\": \"${RekjoringControllerTest.server.baseUrl()}/keys\", " +
                    "\"subject_types_supported\": [\"pairwise\"], " +
                    "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

    WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
            WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
}
