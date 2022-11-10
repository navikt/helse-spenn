package no.nav.helse.opprydding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseName = env["DATABASE_SPENN_OPPRYDDING_DATABASE"]
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"]
            ?: String.format(
                "jdbc:postgresql:///%s?%s&%s",
                databaseName,
                "cloudSqlInstance=${env["GCP_TEAM_PROJECT_ID"]}:${env["DATABASE_REGION"]}:${env["DATABASE_INSTANCE"]}",
                "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            )

        (env["DATABASE_USERNAME"] ?: env["DATABASE_SPENN_OPPRYDDING_USERNAME"])?.let { this.username = it }
        (env["DATABASE_PASSWORD"] ?: env["DATABASE_SPENN_OPPRYDDING_PASSWORD"])?.let { this.password = it }

        maximumPoolSize = 2
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    internal fun getDataSource() = HikariDataSource(hikariConfig)
}
