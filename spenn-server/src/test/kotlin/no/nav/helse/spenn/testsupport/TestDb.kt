package no.nav.helse.spenn.testsupport

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

class TestDb {

    companion object {
        fun createMigratedDSLContext(): DSLContext {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                maximumPoolSize = 2
                //minimumIdle=1
            }
            val flyDS = HikariDataSource(hikariConfig)
            Flyway.configure().dataSource(flyDS)
                    .load()
                    .migrate()
            //flyDS.close()
            return DSL.using(flyDS, SQLDialect.H2)//DSLContext.use(flyDS.connection)
        }
    }
}