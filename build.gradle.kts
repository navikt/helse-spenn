import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

val junitJupiterVersion = "5.4.0"
val tjenestespesifikasjonVersion = "1.2020.04.03-12.49-f6b874c7ef1f"

val githubUser: String by project
val githubPassword: String by project

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.a1c8748")

    implementation("org.flywaydb:flyway-core:6.3.1")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("com.github.seratch:kotliquery:1.3.1")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.2.0")

    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.activation:activation:1.1.1")
    implementation("org.apache.commons:commons-dbcp2:2.5.0")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("org.apache.cxf:cxf-rt-features-logging:3.3.3")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.3.3")
    implementation("org.apache.cxf:cxf-rt-transports-http:3.3.3")
    implementation("org.apache.cxf:cxf-rt-ws-security:3.3.3")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")

    implementation("com.github.navikt:avstemming-v1-tjenestespesifikasjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$tjenestespesifikasjonVersion")

    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.apache.activemq:apache-artemis:2.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir("$buildDir/generated-sources/xsd2java")
        java.srcDir("$buildDir/generated-sources/wsdl2java")
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/tjenestespesifikasjoner-tbd")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "12"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "12"
}

tasks.named<Jar>("jar") {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = "no.nav.helse.spenn.ApplicationKt"
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


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6.4"
}
