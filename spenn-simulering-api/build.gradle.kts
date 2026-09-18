plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spenn.simulering.api.ApplicationKt"
    imageName = "helse-spenn-simulering-api"
}

dependencies {
    api(libs.bundles.logback)

    // for å kunne konsumere request body flere ganger
    api(libs.ktor.server.double.receive)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }

    api(libs.jackson.datatype.jsr310)

    api(libs.tbd.libs.naisful.app)
    api(libs.tbd.libs.azure)
    api(libs.tbd.libs.minimal.soap.client)

    testImplementation(libs.ktor.client.contentnegotiation)
    testImplementation(libs.tbd.libs.naisful.test.app)
    testImplementation(libs.tbd.libs.mock.http.client)
    testImplementation(libs.mockk)
}
