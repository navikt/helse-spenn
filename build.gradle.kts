val junitJupiterVersion = "5.7.1"
val tjenestespesifikasjonVersion = "1.4201aa"
val cxfVersion = "3.3.7"
val testcontainersVersion = "1.16.2"

plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2022.04.04-22.16.0611abb2a604")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")

    implementation("org.flywaydb:flyway-core:8.0.2")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("no.nav:vault-jdbc:1.3.9")
    implementation("com.github.seratch:kotliquery:1.6.0")
    implementation ("io.prometheus:simpleclient_pushgateway:0.9.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.1.0")

    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.3")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.2")
    implementation("javax.activation:activation:1.1.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("org.apache.commons:commons-dbcp2:2.7.0")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")

    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$tjenestespesifikasjonVersion")

    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.apache.activemq:apache-artemis:2.17.0") {
        /* this is a shaded jar that creates conflicts on classpath, see:
            https://github.com/apache/activemq-artemis/blob/181743f3023443d9ea551164b9bbc5d366a3e38f/docs/user-manual/en/client-classpath.md
        */
        exclude("org.apache.activemq", "artemis-jms-client-all")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir("$buildDir/generated-sources/xsd2java")
        java.srcDir("$buildDir/generated-sources/wsdl2java")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    named<Jar>("jar") {
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
