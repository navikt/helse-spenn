plugins {
    kotlin("jvm") version "1.3.61"
}

val junitJupiterVersion = "5.4.0"

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    group = "no.nav.helse"
    version = "0.1-SNAPSHOT"

    setBuildDir("$projectDir/target")

    repositories {
        mavenCentral()
    }

    dependencies {
        api(kotlin("stdlib-jdk8"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
        if (!project.hasProperty("includeBlackBox")) {
            exclude("**/SpennBlackBoxTest*")
        }
    }

    tasks.withType<Wrapper> {
        gradleVersion = "5.6.4"
    }
}

subprojects {
    val githubUser: String by project
    val githubPassword: String by project
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
            url = uri("https://maven.pkg.github.com/navikt/helse-spleis")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }

    dependencies {
        implementation("io.ktor:ktor-jackson:1.2.5")
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
        implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.2.0")
        implementation("org.apache.kafka:kafka-streams:2.3.1")
        implementation("io.confluent:kafka-streams-avro-serde:5.0.0")
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
        implementation("com.google.guava:guava:24.1.1-jre")

        testImplementation("org.apache.cxf.services.sts:cxf-services-sts-core:3.3.3")
        testImplementation("com.h2database:h2:1.4.199")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
        testImplementation("org.mockito:mockito-core:2.23.4")
        testImplementation("io.ktor:ktor-server-test-host:1.2.5")
        testImplementation("com.github.tomakehurst:wiremock:2.24.1")
        testImplementation("io.mockk:mockk:1.9.3")
    }
}
