package no.nav.helse.spenn

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.vault.core.lease.LeaseEndpoints

import org.springframework.vault.core.lease.SecretLeaseContainer
import org.springframework.vault.core.lease.domain.RequestedSecret
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent
import javax.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(name = arrayOf("spring.datasource.url"))
class VaultDbConfig(val container: SecretLeaseContainer, val dataSource: HikariDataSource) {

    val LOG = LoggerFactory.getLogger(VaultDbConfig::class.java)
    @Value("\${vault.postgres.backend}")
    lateinit var vaultPostgresBackend: String
    @Value("\${vault.postgres.role}")
    lateinit var vaultPostgresRole: String

    @PostConstruct
    fun init() {
        container.setLeaseEndpoints(LeaseEndpoints.SysLeases)

        val secret = RequestedSecret.rotating("$vaultPostgresBackend/creds/$vaultPostgresRole")
        container.addLeaseListener {
            if (it.source.equals(secret) && it is SecretLeaseCreatedEvent) {
                LOG.info("Rotating creds for path: " + it.source.path)
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

}