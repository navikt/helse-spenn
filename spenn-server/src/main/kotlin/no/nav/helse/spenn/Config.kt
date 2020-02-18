package no.nav.helse.spenn

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val vaultServiceUserBase = "/var/run/secrets/nais.io/service_user"
const val ourIssuer = "ourissuer"

val vaultServiceUserBasePath: Path = Paths.get(vaultServiceUserBase)

fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(vaultServiceUserBasePath.resolve("username")),
    password = Files.readString(vaultServiceUserBasePath.resolve("password"))
)

fun readEnvironment() = Environment(
    auth = AuthEnvironment(
        acceptedAudience = System.getenv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_ACCEPTED_AUDIENCE"),
        discoveryUrl = URL(System.getenv("NO_NAV_SECURITY_OIDC_ISSUER_OURISSUER_DISCOVERYURL")),
        requiredGroup = System.getenv("API_ACCESS_REQUIREDGROUP")
    )
)

data class ServiceUser(
    val username: String,
    val password: String
)

data class Environment(
    val auth: AuthEnvironment
)

data class AuthEnvironment(
    val acceptedAudience: String,
    val discoveryUrl: URL,
    val requiredGroup: String
)
