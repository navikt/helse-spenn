plugins {
    alias(libs.plugins.sykepenger.root)
    alias(libs.plugins.sykepenger.deployable) apply false
}

allprojects {
    group = "no.nav.helse"
}
