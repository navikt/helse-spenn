package no.nav.helse.spenn.e2e

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spenn.Database
import no.nav.helse.spenn.utbetaling.OppdragDao.Companion.toOppdragDto
import org.flywaydb.core.Flyway

class TestDatabase : Database {
    private val dataSource = run {
        val embeddedPostgres = EmbeddedPostgres.builder().setPort(56789).start()
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
        HikariDataSource(hikariConfig)
    }

    override fun getDataSource() = dataSource

    override fun migrate() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load().also(Flyway::migrate)
    }

    fun resetDatabase() {
        sessionOf(dataSource).use {
            it.run(queryOf("TRUNCATE avstemming CASCADE").asExecute)
            it.run(queryOf("TRUNCATE oppdrag CASCADE").asExecute)
        }
    }

    fun hentAlleOppdrag() = runBlocking {
        using(sessionOf(dataSource)) { session ->
            val query =
                "SELECT * FROM oppdrag"
            session.run(
                queryOf(query).map { it.toOppdragDto() }.asList
            )
        }
    }

}