package no.nav.helse.spenn.e2e

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spenn.Database
import no.nav.helse.spenn.utbetaling.OppdragDao.Companion.toOppdragDto
import javax.sql.DataSource

val databaseContainer = DatabaseContainers.container("spenn-selve", CleanupStrategy.tables("oppdrag,avstemming"))

class TestDatabase(private val dataSource: DataSource) : Database {
    override fun getDataSource() = dataSource

    override fun migrate() {}

    fun hentAlleOppdrag() = runBlocking {
        sessionOf(dataSource).use { session ->
            val query =
                "SELECT * FROM oppdrag"
            session.run(
                queryOf(query).map { it.toOppdragDto() }.asList
            )
        }
    }

}