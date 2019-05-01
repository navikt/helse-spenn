package no.nav.helse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJms
@EnableTransactionManagement
class AppConfig {
    @Bean
    fun appEnv(): Environment {
        return Environment()
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

}