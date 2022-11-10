package no.nav.helse.spenn

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

interface Database {
    fun getDataSource(): DataSource
    fun migrate()
}

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>) : Database {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s",
            requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_DATABASE"]) { "database name must be set if jdbc url is not provided" })
        username = requireNotNull(env["DATABASE_USERNAME"]) { "databasebrukernavn må settes" }
        password = requireNotNull(env["DATABASE_PASSWORD"]) { "databasepassord må settes" }
        maximumPoolSize = 3
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    private val datasource by lazy { HikariDataSource(hikariConfig) }

    override fun getDataSource() = datasource

    override fun migrate() {
        logger.info("Migrerer database")
        Flyway.configure()
            .dataSource(datasource)
            .lockRetryCount(-1)
            .load()
            .migrate()
        logger.info("Migrering ferdig!")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DataSourceBuilder::class.java)
    }
}
