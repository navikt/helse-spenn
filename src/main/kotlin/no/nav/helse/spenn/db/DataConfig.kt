package no.nav.helse.spenn.db

import no.nav.helse.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataConfig(val env: Environment, @Autowired val dataSource: DataSource) {

    @Bean
    fun flywayConfig(): FlywayConfigurationCustomizer {
        return FlywayConfigurationCustomizer { configuration ->
            configuration.initSql(String.format("SET ROLE \"%s-admin\"", env.oppdragDBNavn))
        }
    }

}