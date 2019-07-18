package no.nav.helse.spenn

import com.fasterxml.jackson.databind.ObjectMapper
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
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
@EnableSchedulerLock(defaultLockAtMostFor = "PT2M")
class AppConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return defaultObjectMapper
    }

}