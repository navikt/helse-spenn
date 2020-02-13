dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.61")
}

plugins {
    id("com.github.bjornvester.xjc") version "1.3"
    id("uk.co.boothen.gradle.wsimport") version "0.15"
}

xjc {
    xsdDir.set(layout.projectDirectory.dir("src/main/xsd"))
    xsdFiles = project.files(
            xsdDir.file("grensesnittavstemmingskjema-v1.xsd"),
            xsdDir.file("oppdragskjema-v1.xsd")
    )

    outputJavaDir.set(layout.projectDirectory.dir("target/generated-sources/xsd2java"))
    outputResourcesDir.set(layout.projectDirectory.dir("target/generated-sources/resources"))
}

wsimport {
    wsdlSourceRoot = "/src/main/resources/wsdl"
    generatedSourceRoot = "/generated-sources/wsdl2java"
    generatedClassesRoot = "/classes"

    target = "2.2"
    keep = true
    extension = true
    verbose = false
    quiet = true
    debug = false
    xnocompile = true

    wsdl ("no/nav/system/os/eksponering/simulerFpServiceWSBinding.wsdl")
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir("$buildDir/generated-sources/xsd2java")
        java.srcDir("$buildDir/generated-sources/wsdl2java")
    }
}
