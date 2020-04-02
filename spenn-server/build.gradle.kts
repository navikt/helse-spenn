val mainClass = "no.nav.helse.spenn.ApplicationKt"

dependencies {
    implementation(project(":spenn-core"))
    implementation("com.github.navikt:rapids-and-rivers:1.06d0f27")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.61'")
    //implementation("org.jetbrains.kotlin:kotlin-test-junit:1.3.61")
    implementation("org.eclipse.jetty:jetty-server:9.4.19.v20190610")
    implementation("org.eclipse.jetty:jetty-webapp:9.4.19.v20190610")
    implementation("org.eclipse.jetty:jetty-servlets:9.4.19.v20190610")
    implementation("no.nav.security:token-validation-ktor:1.1.4") {
        exclude("net.minidev", "json-smart")
        exclude("com.nimbusds", "lang-tag")
        exclude("com.nimbusds", "nimbus-jose-jwt")
        implementation("com.nimbusds:lang-tag:1.4.3")
        implementation("net.minidev:json-smart:2.3")
        implementation("com.nimbusds:nimbus-jose-jwt:8.6")
    }
    implementation("com.github.seratch:kotliquery:1.3.1")
    implementation("org.mock-server:mockserver-client-java:5.5.4")

    testImplementation("org.testcontainers:vault:1.12.3")
    testImplementation("org.testcontainers:testcontainers:1.12.3")
    testImplementation("org.testcontainers:postgresql:1.12.3")
    testImplementation("org.testcontainers:kafka:1.12.3")
    testImplementation("org.testcontainers:mockserver:1.12.3")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")
    testImplementation("no.nav.security:token-validation-test-support:1.1.1")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}
