plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spenn.avstemming.ApplicationKt"
    imageName = "helse-spenn-avstemming"
}

dependencies {
    api(libs.flyway.database.postgresql)

    implementation(libs.rapids.and.rivers)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    implementation(libs.ibm.mq.allclient) {
        exclude("com.fasterxml.jackson.core", "jackson-core")
        exclude("com.fasterxml.jackson.core", "jackson-annotations")
        exclude("com.fasterxml.jackson.core", "jackson-databind")
    }

    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.jaxb.runtime)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
    testImplementation(libs.mockk)
}
