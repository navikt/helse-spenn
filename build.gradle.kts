plugins {
    alias(libs.plugins.sas.root)
    alias(libs.plugins.sas.deployable) apply false
}

allprojects {
    group = "no.nav.helse"
}
