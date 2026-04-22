import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val junitJupiterVersion = "5.12.1"
val mockkVersion = "1.13.17"
val jacksonVersion = "2.18.3"
val flywayVersion = "11.5.0"
val hikariCPVersion = "6.3.0"
val tbdLibsVersion = "20260416.1520"
val rapidsAndRiversVersion = "2026011411051768385145.e8ebad1177b4"
val postgresqlVersion = "42.7.7"
val kotliqueryVersion = "1.9.0"
val cloudSqlVersion = "1.21.0"

plugins {
    kotlin("jvm") version "2.3.20"
}

allprojects {
    group = "no.nav.helse"

    // Sett opp repositories basert på om vi kjører i CI eller ikke
    // Jf. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
    repositories {
        mavenCentral()
        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            maven {
                url = uri("https://maven.pkg.github.com/navikt/maven-release")
                credentials {
                    username = "token"
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull!!
                }
            }
        } else {
            maven("https://repo.adeo.no/repository/github-package-registry-navikt/")
        }
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
    ext.set("cloudSqlVersion", cloudSqlVersion)
    ext.set("rapidsAndRiversVersion", rapidsAndRiversVersion)
    ext.set("tbdLibsVersion", tbdLibsVersion)
    ext.set("hikariCPVersion", hikariCPVersion)
    ext.set("flywayVersion", flywayVersion)
    ext.set("jacksonVersion", jacksonVersion)
    ext.set("mockkVersion", mockkVersion)

    configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of("21"))
        }
    }

    tasks {
        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}
