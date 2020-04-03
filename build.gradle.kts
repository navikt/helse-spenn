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
    implementation("com.github.navikt:rapids-and-rivers:1.db77acd")

    implementation("org.flywaydb:flyway-core:6.3.1")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("com.github.seratch:kotliquery:1.3.1")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.2.0")

    implementation("org.apache.cxf:cxf-rt-ws-security:3.3.3")
    implementation("org.apache.cxf:cxf-rt-ws-policy:3.3.3")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.3.3")
    implementation("org.apache.cxf:cxf-rt-features-logging:3.3.3")
    implementation("org.apache.cxf:cxf-rt-transports-http:3.3.3")
    implementation("javax.activation:activation:1.1.1")
    implementation("com.sun.xml.ws:jaxws-rt:2.3.2")
    implementation("javax.xml.stream:stax-api:1.0-2")

    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.activation:activation:1.1.1")

    implementation("com.github.navikt:avstemming-v1-tjenestespesifikasjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$tjenestespesifikasjonVersion")

    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")
    testImplementation("io.mockk:mockk:1.9.3")

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

/*dependencies {
    implementation("org.apache.cxf:cxf-rt-ws-security:3.3.3")
    implementation("org.apache.cxf:cxf-rt-ws-policy:3.3.3")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.3.3")
    implementation("org.apache.cxf:cxf-rt-features-logging:3.3.3")
    implementation("org.apache.cxf:cxf-rt-transports-http:3.3.3")
    implementation("javax.activation:activation:1.1.1")
    implementation("com.sun.xml.ws:jaxws-rt:2.3.2")

    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.activation:activation:1.1.1")

    implementation("io.ktor:ktor-jackson:1.2.5")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.2.0")
    implementation("org.apache.kafka:kafka-streams:2.3.1")
    implementation("io.confluent:kafka-streams-avro-serde:5.0.0")

    implementation("org.apache.commons:commons-dbcp2:2.5.0")
    implementation("org.flywaydb:flyway-core:5.2.4")


    implementation("net.javacrumbs.shedlock:shedlock-core:3.0.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:3.0.0")
    implementation("com.zaxxer:HikariCP:3.4.1")
    implementation("org.postgresql:postgresql:42.2.8")
    implementation("no.nav:vault-jdbc:1.3.1")
    implementation("khttp:khttp:1.0.0")
    implementation("com.google.guava:guava:24.1.1-jre")

    testImplementation("org.apache.cxf.services.sts:cxf-services-sts-core:3.3.3")
    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.ktor:ktor-server-test-host:1.2.5")
    testImplementation("com.github.tomakehurst:wiremock:2.24.1")
    testImplementation("io.mockk:mockk:1.9.3")
} */
