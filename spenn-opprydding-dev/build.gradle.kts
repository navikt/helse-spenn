val mainClass = "no.nav.helse.opprydding.AppKt"
val testcontainersVersion = "1.17.4"

dependencies {

    implementation("com.google.cloud.sql:postgres-socket-factory:1.7.2")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation(project(":spenn-selve"))
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
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
