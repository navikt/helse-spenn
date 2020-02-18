package no.nav.helse.spenn

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val vaultServiceUserBase = "/var/run/secrets/nais.io/service_user"

val vaultServiceUserBasePath: Path = Paths.get(vaultServiceUserBase)

fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(vaultServiceUserBasePath.resolve("username")),
    password = Files.readString(vaultServiceUserBasePath.resolve("password"))
)

data class ServiceUser(
    val username: String,
    val password: String
)
