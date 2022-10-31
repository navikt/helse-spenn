package no.nav.helse.opprydding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import java.time.Duration
import javax.sql.DataSource

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseName = env["DATABASE_NAME"] ?: env["DATABASE_SPENN_OPPRYDDING_DATABASE"]

    private val vaultMountPath = env["VAULT_MOUNTPATH"]
    private val shouldGetCredentialsFromVault = vaultMountPath != null

    // username and password is only needed when vault is not enabled,
    // since we rotate credentials automatically when vault is enabled
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s%s",
            requireNotNull(env["DATABASE_HOST"] ?: env["DATABASE_SPENN_OPPRYDDING_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_PORT"] ?: env["DATABASE_SPENN_OPPRYDDING_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" },
            (env["DATABASE_USERNAME"] ?: env["DATABASE_SPENN_OPPRYDDING_USERNAME"])?.let { "?user=$it" } ?: "")

        (env["DATABASE_USERNAME"] ?: env["DATABASE_SPENN_OPPRYDDING_USERNAME"])?.let { this.username = it }
        (env["DATABASE_PASSWORD"] ?: env["DATABASE_SPENN_OPPRYDDING_PASSWORD"])?.let { this.password = it }

        maximumPoolSize = 3
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    init {
        if (!shouldGetCredentialsFromVault) {
            if (!env.containsKey("DATABASE_JDBC_URL")) {
                checkNotNull(env["DATABASE_USERNAME"] ?: env["DATABASE_SPENN_OPPRYDDING_USERNAME"]) { "username must be set when vault is disabled" }
                checkNotNull(env["DATABASE_PASSWORD"] ?: env["DATABASE_SPENN_OPPRYDDING_PASSWORD"]) { "password must be set when vault is disabled" }
            }
        } else {
            check(null == env["DATABASE_USERNAME"]) { "username must not be set when vault is enabled" }
            check(null == env["DATABASE_PASSWORD"]) { "password must not be set when vault is enabled" }
            checkNotNull(env["DATABASE_NAME"]) { "database name must be set when vault is enabled" }
        }
    }

    internal fun getDataSource(): DataSource =
        getDataSource(Role.User)

    private fun getDataSource(role: Role = Role.User): DataSource {
        if (!shouldGetCredentialsFromVault) return HikariDataSource(hikariConfig)
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            vaultMountPath,
            "$databaseName-$role"
        )
    }

    private enum class Role {
        Admin, User, ReadOnly;

        override fun toString() = name.lowercase()
    }
}
