val junitJupiterVersion = "5.9.1"
val testcontainersVersion = "1.17.4"

plugins {
    kotlin("jvm") version "1.7.20"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:2022092314391663936769.9d5d33074875")

        implementation("org.flywaydb:flyway-core:8.0.2")
        implementation("no.nav:vault-jdbc:1.3.10") // hikari og postgres kommer med på kjøpet
        implementation("com.github.seratch:kotliquery:1.9.0")

        implementation("com.google.cloud.sql:postgres-socket-factory:1.7.0")

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
        testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
        testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    }

}

subprojects {
    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "17"
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        withType<Wrapper> {
            gradleVersion = "7.4"
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}

tasks {
    named("build") {
        finalizedBy()
        project.buildDir.deleteRecursively()
    }
}
