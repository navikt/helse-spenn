val mainClass = "no.nav.helse.spenn.avstemming.ApplicationKt"
val testcontainersVersion = "1.19.0"

dependencies {
    api("org.flywaydb:flyway-core:9.7.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.github.seratch:kotliquery:1.9.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.0.1")

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.1")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.mockk:mockk:1.13.9")
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
