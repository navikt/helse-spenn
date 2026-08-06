plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spenn.ApplicationKt"
    imageName = "helse-spenn-simulering"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.tbd.libs.azure)
    implementation(libs.tbd.libs.spenn.simulering.client)
    implementation(libs.tbd.libs.retry)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)
}
