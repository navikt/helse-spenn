plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spenn.ApplicationKt"
    imageName = "helse-spenn-selve"
}

dependencies {
    api(libs.flyway.database.postgresql)

    implementation(libs.rapids.and.rivers)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
    testImplementation(libs.mockk)
}
