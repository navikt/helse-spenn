package no.nav.helse.opprydding

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class PersonRepository(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {tx ->
                tx.finnAvstemmingsnøkkel(fødselsnummer).also { tx.slettAvstemming(it) }
                tx.slettOppdrag(fødselsnummer)
            }
        }
    }

    private fun TransactionalSession.finnAvstemmingsnøkkel(fødselsnummer: String): List<Long> {
        @Language("PostgreSQL")
        val query = "SELECT avstemmingsnokkel FROM oppdrag WHERE fnr = ?"
        return run(queryOf(query, fødselsnummer).map { it.long("avstemmingsnokkel") }.asList)
    }

    private fun TransactionalSession.slettAvstemming(avstemmingsnøkler: List<Long>) {
        if (avstemmingsnøkler.isEmpty()) return
        @Language("PostgreSQL")
        val query = "DELETE FROM avstemming WHERE avstemmingsnokkel_tom in (${avstemmingsnøkler.joinToString { "?" }})"
        run(queryOf(query, *avstemmingsnøkler.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.slettOppdrag(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM oppdrag WHERE fnr = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }
}