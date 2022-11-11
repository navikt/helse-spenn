val mainClass = "no.nav.helse.spenn.ApplicationKt"
val testcontainersVersion = "1.17.4"

dependencies {
    api("org.flywaydb:flyway-core:9.7.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.7.2") // spenn-avstemming
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")

    implementation ("io.prometheus:simpleclient_pushgateway:0.16.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.0.1")

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.1")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.apache.activemq:apache-artemis:2.17.0") {
        /* this is a shaded jar that creates conflicts on classpath, see:
            https://github.com/apache/activemq-artemis/blob/181743f3023443d9ea551164b9bbc5d366a3e38f/docs/user-manual/en/client-classpath.md
        */
        exclude("org.apache.activemq", "artemis-jms-client-all")
        exclude("javax.xml.bind", "jaxb-api")
        exclude("com.sun.xml.bind", "jaxb-impl")
        exclude("com.sun.xml.bind", "jaxb-jxc")
    }
}

tasks {
    named<Jar>("jar") {
        archiveFileName.set("app.jar")

        manifest {
            attributes["Main-Class"] = mainClass
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
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir("$buildDir/generated-sources/xsd2java")
        java.srcDir("$buildDir/generated-sources/wsdl2java")
    }
}
