val junitJupiterVersion = "5.11.0"
val mockkVersion = "1.13.12"
val jacksonVersion = "2.18.1"
val flywayVersion = "10.17.1"
val hikariCPVersion = "5.1.0"
val tbdLibsVersion = "2024.11.04-14.04-cfd83570"
val rapidsAndRiversVersion = "2024082209261724311613.5baa691b9e0a"
val postgresqlVersion = "42.7.3"
val kotliqueryVersion = "1.9.0"
val postgresSocketFactoryVersion = "1.20.0"

val jvmTarget = 21

plugins {
    kotlin("jvm") version "2.0.10"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        val githubPassword: String? by project
        mavenCentral()
        /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
            så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
            Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
         */
        maven {
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

subprojects {
    ext.set("postgresqlVersion", postgresqlVersion)
    ext.set("kotliqueryVersion", kotliqueryVersion)
    ext.set("postgresSocketFactoryVersion", postgresSocketFactoryVersion)
    ext.set("rapidsAndRiversVersion", rapidsAndRiversVersion)
    ext.set("tbdLibsVersion", tbdLibsVersion)
    ext.set("hikariCPVersion", hikariCPVersion)
    ext.set("flywayVersion", flywayVersion)
    ext.set("jacksonVersion", jacksonVersion)
    ext.set("mockkVersion", mockkVersion)

    tasks {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(jvmTarget)
            }
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        withType<Wrapper> {
            gradleVersion = "8.10"
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
