val mainClass = "no.nav.helse.spenn.ApplicationKt"
val tjenestespesifikasjonVersion = "1.4201aa"
val cxfVersion = "4.0.0-SNAPSHOT"

dependencies {
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.1")

    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion") {
        // så lenge vi er avhengig av cxf 4.0.0-SNAPSHOT
        exclude("org.opensaml", "opensaml-saml-impl")
        exclude("org.opensaml", "opensaml-xacml-impl")
        exclude("org.opensaml", "opensaml-xacml-saml-impl")
    }

    // så lenge vi er avhengig av cxf 4.0.0-SNAPSHOT
    implementation("org.opensaml:opensaml-saml-impl:4.0.1")
    implementation("org.opensaml:opensaml-xacml-impl:4.0.1")
    implementation("org.opensaml:opensaml-xacml-saml-impl:4.0.1")

    testImplementation("io.mockk:mockk:1.12.0")
}

repositories {
    maven("https://repository.apache.org/snapshots/") // så lenge vi er avhengig av cxf 4.0.0-SNAPSHOT
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
    }
}
