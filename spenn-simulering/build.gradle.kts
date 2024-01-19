val mainClass = "no.nav.helse.spenn.ApplicationKt"
val tbdLibsVersion = "2024.01.14-10.01-dff616a3"

dependencies {
    implementation("com.github.navikt.tbd-libs:minimal-soap-client:$tbdLibsVersion")

    testImplementation("com.github.navikt.tbd-libs:mock-http-client:$tbdLibsVersion")
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
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}
