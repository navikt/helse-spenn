package no.nav.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJms
@EnableTransactionManagement
@EnableScheduling
class AppConfig {
    @Bean
    fun appEnv(): Environment {
        return Environment()
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return defaultObjectMapper
    }

}