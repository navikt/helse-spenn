plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spenn.oppdrag.ApplicationKt"
    imageName = "helse-spenn-mq"
}

dependencies {
    api(libs.jackson.dataformat.xml)

    implementation(libs.rapids.and.rivers)

    implementation(libs.ibm.mq.allclient) {
        exclude("com.fasterxml.jackson.core", "jackson-core")
        exclude("com.fasterxml.jackson.core", "jackson-annotations")
        exclude("com.fasterxml.jackson.core", "jackson-databind")
    }

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)
    testImplementation(libs.apache.artemis) {
        /* this is a shaded jar that creates conflicts on classpath, see:
            https://github.com/apache/activemq-artemis/blob/181743f3023443d9ea551164b9bbc5d366a3e38f/docs/user-manual/en/client-classpath.md
         */
        exclude("org.apache.activemq", "artemis-jms-client-all")
        exclude("javax.xml.bind", "jaxb-api")
        exclude("com.sun.xml.bind", "jaxb-impl")
        exclude("com.sun.xml.bind", "jaxb-jxc")
    }
}
