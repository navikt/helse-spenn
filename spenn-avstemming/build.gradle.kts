val mainClass = "no.nav.helse.spenn.avstemming.ApplicationKt"

val rapidsAndRiversVersion: String by project
val flywayVersion: String by project
val hikariCPVersion: String by project
val postgresqlVersion: String by project
val kotliqueryVersion: String by project
val mockkVersion: String by project
val tbdLibsVersion: String by project

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")

    api("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.4.0.0") {
        exclude("com.fasterxml.jackson.core", "jackson-core")
        exclude("com.fasterxml.jackson.core", "jackson-annotations")
        exclude("com.fasterxml.jackson.core", "jackson-databind")
    }

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks {
    named<Jar>("jar") {
        archiveFileName.set("app.jar")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
    }
}
