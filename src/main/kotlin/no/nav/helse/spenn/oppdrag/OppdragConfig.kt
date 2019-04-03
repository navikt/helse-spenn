package no.nav.helse.spenn.oppdrag

import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OppdragConfig() {

    @Bean
    fun objectFactory(): ObjectFactory {
        return ObjectFactory()
    }

}