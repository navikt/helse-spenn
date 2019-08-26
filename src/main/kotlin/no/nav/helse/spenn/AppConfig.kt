package no.nav.helse.spenn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

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

    @Bean
    fun lockProvider(dataSource: HikariDataSource): LockProvider {
        return JdbcTemplateLockProvider(dataSource);
    }

}