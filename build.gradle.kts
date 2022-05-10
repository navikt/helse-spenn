val junitJupiterVersion = "5.7.1"
val testcontainersVersion = "1.16.2"

plugins {
    kotlin("jvm") version "1.6.10"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:2022.05.09-12.50.569dc0a4e492")

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
