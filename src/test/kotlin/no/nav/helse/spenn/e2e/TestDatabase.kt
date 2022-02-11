package no.nav.helse.spenn.e2e

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spenn.Database
import no.nav.helse.spenn.utbetaling.OppdragDao.Companion.toOppdragDto
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class TestDatabase : Database {
    private val dataSource = run {
        val postgres = PostgreSQLContainer<Nothing>("postgres:13").also { it.start() }
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
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