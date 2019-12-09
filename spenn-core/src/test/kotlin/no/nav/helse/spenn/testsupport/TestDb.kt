package no.nav.helse.spenn.testsupport

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

class TestDb {

    companion object {
        fun createMigratedDataSource(): HikariDataSource {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                maximumPoolSize = 2
            }
            val flyDS = HikariDataSource(hikariConfig)
            Flyway.configure().dataSource(flyDS)
                .locations("db/migration")
                .target("8")
                .load()
                .migrate()
            return flyDS
        }
    }
}