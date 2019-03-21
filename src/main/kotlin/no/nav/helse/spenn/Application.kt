package no.nav.helse.spenn

import no.nav.helse.integrasjon.okonomi.oppdrag.Environment
import no.nav.helse.integrasjon.okonomi.oppdrag.VedtakListener
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jms.annotation.EnableJms

@SpringBootApplication
@EnableJms
class Application
fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setBannerMode(Banner.Mode.OFF)
        VedtakListener(Environment()).start()
    }
}