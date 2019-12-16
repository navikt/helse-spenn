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
            val dataSource = HikariDataSource(hikariConfig)
            Flyway.configure().dataSource(dataSource)
                .locations("db/migration")
                .target("8")
                .load()
                .migrate()
            dataSource.connection.use { connection ->
                connection.prepareStatement("delete from transaksjon").executeUpdate()
                connection.prepareStatement("delete from oppdrag").executeUpdate()
            }
            return dataSource
        }
    }
}