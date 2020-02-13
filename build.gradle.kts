plugins {
    kotlin("jvm") version "1.3.61"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    group = "no.nav.helse"
    version = "0.1-SNAPSHOT"

    setBuildDir("$projectDir/target")

    repositories{
        mavenCentral()
    }

    dependencies{
        api(kotlin("stdlib-jdk8"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_12
        targetCompatibility = JavaVersion.VERSION_12
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "12"
    }

    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "12"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        exclude("**/SpennBlackBoxTest*")

    }

    tasks.withType<Wrapper> {
        gradleVersion = "5.6.4"
    }
}

subprojects {
//    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    repositories {
        jcenter()
        maven {
            url = uri("https://jcenter.bintray.com/")
        }

        maven {
            url = uri("http://packages.confluent.io/maven/")
        }

        maven {
            url = uri("https://kotlin.bintray.com/kotlinx")
        }

        maven {
            url = uri("http://repo.maven.apache.org/maven2")
        }
    }

    tasks {
        "test"(Test::class) {
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61")
        implementation("io.ktor:ktor-server-netty:1.2.5")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.0")
        implementation("io.ktor:ktor-jackson:1.2.5")
        implementation("io.ktor:ktor-metrics-micrometer:1.2.5")
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
        implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.2.0")
        implementation("org.apache.kafka:kafka-streams:2.3.1")
        implementation("io.confluent:kafka-streams-avro-serde:5.0.0")
        implementation("io.micrometer:micrometer-registry-prometheus:1.1.6")
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("net.logstash.logback:logstash-logback-encoder:6.2")
        implementation("javax.validation:validation-api:2.0.1.Final")
        implementation("javax.xml.bind:jaxb-api:2.3.1")
        implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
        implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
        implementation("javax.annotation:javax.annotation-api:1.3.2")
        implementation("javax.activation:activation:1.1.1")
        implementation("org.apache.commons:commons-dbcp2:2.5.0")
        implementation("org.flywaydb:flyway-core:5.2.4")
        implementation("javax.xml.stream:stax-api:1.0-2")
        implementation("org.apache.cxf:cxf-rt-features-logging:3.3.3")
        implementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.3.3")
        implementation("org.apache.cxf:cxf-rt-transports-http:3.3.3")
        implementation("org.apache.cxf:cxf-rt-ws-security:3.3.3")
        implementation("javax.xml.ws:jaxws-api:2.3.1")
        implementation("javax.jws:javax.jws-api:1.1")
        implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
        implementation("net.javacrumbs.shedlock:shedlock-core:3.0.0")
        implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:3.0.0")
        implementation("com.zaxxer:HikariCP:3.4.1")
        implementation("org.postgresql:postgresql:42.2.8")
        implementation("no.nav:vault-jdbc:1.3.1")
        implementation("khttp:khttp:1.0.0")
        implementation("no.nav.security:token-validation-ktor:1.1.4") {
            exclude("net.minidev", "json-smart")
            exclude("com.nimbusds", "lang-tag")
            exclude("com.nimbusds", "nimbus-jose-jwt")
            implementation("com.nimbusds:lang-tag:1.4.3")
            implementation("net.minidev:json-smart:2.3")
            implementation("com.nimbusds:nimbus-jose-jwt:8.6")
        }
        implementation("org.mock-server:mockserver-client-java:5.5.4")
        implementation("com.google.guava:guava:24.1.1-jre")
        testImplementation("org.apache.cxf.services.sts:cxf-services-sts-core:3.3.3")
        testImplementation("com.h2database:h2:1.4.199")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.2")
        testImplementation("org.jetbrains.kotlin:kotlin-test:1.3.61")
        testImplementation("org.testcontainers:vault:1.12.3")
        testImplementation("org.testcontainers:testcontainers:1.12.3")
        testImplementation("org.testcontainers:postgresql:1.12.3")
        testImplementation("org.testcontainers:kafka:1.12.3")
        testImplementation("org.testcontainers:mockserver:1.12.3")
        testImplementation("org.mockito:mockito-core:2.23.4")
        testImplementation("io.ktor:ktor-server-test-host:1.2.5")
        testImplementation("no.nav.security:token-validation-test-support:1.1.1")
        testImplementation("com.github.tomakehurst:wiremock:2.24.1")
    }
}