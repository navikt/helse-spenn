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

    api(libs.tbd.libs.naisful.app)
    api(libs.tbd.libs.azure)
    implementation(libs.tbdLibs.resultObject)
    implementation(platform(libs.jackson3.bom))
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml")

    testImplementation(libs.ktor.client.contentnegotiation)
    testImplementation(libs.httpclient5.fluent)
    testImplementation(libs.tbd.libs.naisful.test.app)
    testImplementation(libs.tbd.libs.mock.http.client)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.sykepengerLibs.testing)
    testImplementation(libs.wiremock)
}
