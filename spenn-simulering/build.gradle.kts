val mainClass = "no.nav.helse.spenn.ApplicationKt"
val tjenestespesifikasjonVersion = "1.4201aa"
val cxfVersion = "3.5.3"

dependencies {
    implementation("com.sun.xml.bind:jaxb-core:4.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:4.0.1")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.2")
    implementation("javax.activation:activation:1.1.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")

    implementation("com.github.navikt.tjenestespesifikasjoner:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$tjenestespesifikasjonVersion")

    testImplementation("io.mockk:mockk:1.12.0")
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
