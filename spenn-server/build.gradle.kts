val mainClass = "no.nav.helse.spenn.ApplicationKt"

dependencies {
    implementation(project(":spenn-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.61'")
    implementation("org.jetbrains.kotlin:kotlin-test-junit:1.3.61")
    implementation("org.eclipse.jetty:jetty-server:9.4.19.v20190610")
    implementation("org.eclipse.jetty:jetty-webapp:9.4.19.v20190610")
    implementation("org.eclipse.jetty:jetty-servlets:9.4.19.v20190610")
}

tasks.named<Jar>("jar") {
    baseName = "helse-spenn"

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