import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "1.7.25"
val prometheusVersion = "0.5.0"
val gsonVersion = "2.7"
val navStreamsVersion = "1a24b7e"
val fuelVersion = "1.15.1"

val junitJupiterVersion = "5.3.1"
val assertJVersion = "3.11.1"
val jacksonVersion = "2.9.8"
val wireMockVersion = "2.19.0"
val mockkVersion="1.9"
val springBootVersion="2.1.3.RELEASE"
val kafkaVersion="2.0.1"
val confluentVersion="5.0.0"

plugins {
    kotlin("jvm") version "1.3.20"
    id("org.springframework.boot") version "2.1.3.RELEASE"
    kotlin("plugin.spring") version "1.3.20"
}

apply(plugin = "io.spring.dependency-management")

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }

}

val jaxb by configurations.creating
val jaxbTargetDir = file("$buildDir/generated/src/main/java")

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("org.apache.kafka:kafka-streams:$kafkaVersion")
    compile("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    // jdk 11 removed all jaxb, xml support jars
    compile("javax.validation:validation-api:2.0.1.Final")
    compile("javax.xml.bind:jaxb-api:2.3.1")
    compile("com.sun.xml.bind:jaxb-core:2.3.0.1")
    compile("com.sun.xml.bind:jaxb-impl:2.3.2")
    compile("javax.annotation:javax.annotation-api:1.3.2")
    compile("javax.activation:activation:1.1")

    compile("org.springframework.boot:spring-boot-starter-web")
    compile("com.ibm.mq:mq-jms-spring-boot-starter:2.0.0")
    testCompile("org.springframework.boot:spring-boot-starter-test")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testCompile("org.assertj:assertj-core:$assertJVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    jaxb ("javax.xml.bind:jaxb-api:2.3.1")
    jaxb ("com.sun.xml.bind:jaxb-impl:2.3.2")
    jaxb ("com.sun.xml.bind:jaxb-xjc:2.3.2")
    jaxb ("com.sun.xml.bind:jaxb-osgi:2.3.2")
    jaxb ("javax.activation:activation:1.1")
    jaxb ("com.sun.xml.bind:jaxb-core:2.3.0.1")

}

repositories {
    jcenter()
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
    mavenLocal()
}

tasks {
    register("generateSources") {
        doLast {

            if (!jaxbTargetDir.exists()) {
                jaxbTargetDir.mkdirs()
            }
            ant.withGroovyBuilder {
                "taskdef"("name" to "xjc", "classname" to "com.sun.tools.xjc.XJCTask",
                        "classpath" to jaxb.asPath )
                "xjc"("destdir" to jaxbTargetDir,
                        "schema" to "$projectDir/src/main/xsd/oppdragskjema-v1.xsd",
                        "removeOldOutput" to "yes", "extension" to "true") {
                    //"arg"("line" to "-nv -disableXmlSecurity")
                }
            }
        }
    }
}
sourceSets["main"].java.srcDir(jaxbTargetDir)

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
       // attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    doLast {
        configurations["compile"].forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
    dependsOn("generateSources")
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.1.1"
}
