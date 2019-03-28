package no.nav.helse.spenn.db

import no.nav.helse.Environment
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@Configuration
@EnableJdbcRepositories
class DataConfig(val env: Environment) {

    @Bean
    fun flyway(): FlywayConfigurationCustomizer {
        return FlywayConfigurationCustomizer { configuration ->
            configuration.initSql(String.format("SET ROLE \"%s-admin\"", env.oppdragDBNavn))
        }
    }

}