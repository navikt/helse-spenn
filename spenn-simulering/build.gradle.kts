val mainClass = "no.nav.helse.spenn.ApplicationKt"
val cxfVersion = "4.0.0"

dependencies {
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.1")

    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    testImplementation("io.mockk:mockk:1.13.9")
}

repositories {
    // org.apache.cxf:cxf-rt-ws-security:4.0.0 er avhengig av opensaml-xacml-saml-impl:4.2.0
    // som i skrivende stund ikke er tilgjengelig på maven central, men i shibboleth
    maven("https://build.shibboleth.net/nexus/content/repositories/releases/") // så lenge vi er avhengig av cxf 4.0.0-SNAPSHOT
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
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
    }
}
