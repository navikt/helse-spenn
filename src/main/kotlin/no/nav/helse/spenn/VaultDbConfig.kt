package no.nav.helse.spenn

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.core.lease.LeaseEndpoints

import org.springframework.vault.core.lease.SecretLeaseContainer
import org.springframework.vault.core.lease.domain.RequestedSecret
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent
import javax.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(name = ["spring.cloud.vault.enabled"], havingValue="true")
class VaultDbConfig(val container: SecretLeaseContainer,
                    val dataSource: HikariDataSource,
                    @Value("\${spring.cloud.vault.database.backend}")
                    val vaultPostgresBackend: String,
                    @Value("\${spring.cloud.vault.database.role}")
                    val vaultPostgresRole: String,
                    val vaultTemplate: VaultTemplate,
                    @Value("\${spring.datasource.url}")
                    val jdbcURL: String) {
    companion object {
        private val LOG = LoggerFactory.getLogger(VaultDbConfig::class.java)
    }

    @PostConstruct
    fun init() {
        container.setLeaseEndpoints(LeaseEndpoints.SysLeases)

        val secret = RequestedSecret.rotating("$vaultPostgresBackend/creds/$vaultPostgresRole")
        container.addLeaseListener {
            if (it.source.equals(secret) && it is SecretLeaseCreatedEvent) {
                LOG.info("Rotating creds for path: ${it.source.path}")
                val username = it.secrets.get("username").toString()
                val password = it.secrets.get("password").toString()
                dataSource.username = username
                dataSource.password = password
                dataSource.hikariConfigMXBean.setUsername(username)
                dataSource.hikariConfigMXBean.setPassword(password)
            }
        }
        container.addRequestedSecret(secret)
    }

    @Bean
    @Profile(value= ["preprod","prod"])
    fun flywayMigrationStrategy(): FlywayMigrationStrategy = FlywayMigrationStrategy {
        LOG.info("init flyway migration strategy")
        val response = vaultTemplate.read("$vaultPostgresBackend/creds/helse-spenn-oppdrag-admin")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = jdbcURL
            maximumPoolSize=1
            minimumIdle=1
            username = response.data?.get("username").toString()
            password = response.data?.get("password").toString()
        }
        val flyDS = HikariDataSource(hikariConfig)
        Flyway.configure().dataSource(flyDS)
                .initSql("SET ROLE \"helse-spenn-oppdrag-admin\"")
                .load()
                .migrate()
        flyDS.close()
    }

}