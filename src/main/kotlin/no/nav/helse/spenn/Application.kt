package no.nav.helse.spenn

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableOIDCTokenValidation
@SpringBootApplication(scanBasePackages = ["no.nav.helse"])
class Application
fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}