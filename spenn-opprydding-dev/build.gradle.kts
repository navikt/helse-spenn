plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.opprydding.AppKt"
    imageName = "helse-spenn-opprydding"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.cloud.sql.postgres.socket.factory)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    testImplementation(project(":spenn-selve"))
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
}
