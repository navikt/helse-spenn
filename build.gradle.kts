import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val junitJupiterVersion = "5.11.3"
val mockkVersion = "1.13.12"
val jacksonVersion = "2.18.1"
val flywayVersion = "10.21.0"
val hikariCPVersion = "6.1.0"
val tbdLibsVersion = "2024.11.25-10.59-6f263a10"
val rapidsAndRiversVersion = "2024120608421733470968.fc0a3d5e3685"
val postgresqlVersion = "42.7.4"
val kotliqueryVersion = "1.9.0"
val cloudSqlVersion = "1.21.0"

plugins {
    kotlin("jvm") version "2.0.21"
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