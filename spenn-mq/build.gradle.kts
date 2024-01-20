val mainClass = "no.nav.helse.spenn.oppdrag.ApplicationKt"

val jacksonVersion = "2.16.1"


dependencies {
    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.2.0")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")

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
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir("${layout.buildDirectory.get()}/generated-sources/xsd2java")
        java.srcDir("${layout.buildDirectory.get()}/generated-sources/wsdl2java")
    }
}
