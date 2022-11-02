val mainClass = "no.nav.helse.spenn.ApplicationKt"
val tjenestespesifikasjonVersion = "1.4201aa"
val cxfVersion = "3.5.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")

    implementation ("io.prometheus:simpleclient_pushgateway:0.16.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.0.1")

    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")

    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:$tjenestespesifikasjonVersion")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$tjenestespesifikasjonVersion")

    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.apache.activemq:apache-artemis:2.17.0") {
        /* this is a shaded jar that creates conflicts on classpath, see:
            https://github.com/apache/activemq-artemis/blob/181743f3023443d9ea551164b9bbc5d366a3e38f/docs/user-manual/en/client-classpath.md
        */
        exclude("org.apache.activemq", "artemis-jms-client-all")
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
