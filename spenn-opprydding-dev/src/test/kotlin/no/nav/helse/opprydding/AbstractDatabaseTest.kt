package no.nav.helse.opprydding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import javax.sql.DataSource

internal abstract class AbstractDatabaseTest {

    protected val personRepository = PersonRepository(dataSource)

    protected companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
            withReuse(true)
            withLabel("app-navn", "spenn")
            start()
        }

        val dataSource =
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 5
                minimumIdle = 1
                idleTimeout = 500001
                connectionTimeout = 10000
                maxLifetime = 600001
                initializationFailTimeout = 5000
            })

        private fun createTruncateFunction(dataSource: DataSource) {
            sessionOf(dataSource).use {
                @Language("PostgreSQL")
                val query = """
            CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
            DECLARE
            truncate_statement text;
            BEGIN
                SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' CASCADE'
                    INTO truncate_statement
                FROM pg_tables
                WHERE schemaname='public'
                AND tablename not in ('flyway_schema_history');

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
        """
                it.run(queryOf(query).asExecute)
            }
        }

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()

            createTruncateFunction(dataSource)
        }
    }

    protected fun assertTabellinnhold(booleanExpressionBlock: (actualTabellCount: Int) -> Boolean) {
        val tabeller = finnTabeller().toMutableList()
        tabeller.remove("flyway_schema_history")
        tabeller.forEach {
            val tabellCount = finnTabellCount(it)
            assertTrue(booleanExpressionBlock(tabellCount)) { "$it has $tabellCount rows" }
        }
    }

    private fun finnTabeller(): List<String> {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            session.run(queryOf(query).map { it.string("table_name") }.asList)
        }
    }

    private fun finnTabellCount(tabellnavn: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM $tabellnavn"
            session.run(queryOf(query).map { it.int(1) }.asSingle) ?: 0
        }
    }

    @BeforeEach
    fun resetDatabase() {
        sessionOf(dataSource).use {
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }

    protected fun opprettPerson(fødselsnummer: String, avstemmingsnøkkel: Int) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.opprettOppdrag(fødselsnummer, avstemmingsnøkkel)
                tx.opprettAvstemming(avstemmingsnøkkel)
            }
        }
    }

    private fun TransactionalSession.opprettOppdrag(fødselsnummer: String, avstemmingsnøkkel: Int) {
        @Language("PostgreSQL")
        val query = """INSERT INTO oppdrag(
                avstemmingsnokkel, fagomrade, fnr, mottaker, opprettet, endret, fagsystem_id, status, 
                avstemt, totalbelop, beskrivelse, feilkode_oppdrag, behov, oppdrag_response, orgnr, utbetaling_id
                ) VALUES (?, 'SPREF', ?, 'mottaker', now(), now(), 'fagsystemid', 'status', false, 1000, 'beskrivelse', 'feilkode', '{}'::json, 'response', 'organisasjonsnummer', ?)
                """
        run(queryOf(query, avstemmingsnøkkel, fødselsnummer, UUID.randomUUID()).asExecute)
    }

    private fun TransactionalSession.opprettAvstemming(avstemmingsnøkkel: Int) {
        @Language("PostgreSQL")
        val query = """INSERT INTO avstemming(id, fagomrade, avstemmingsnokkel_tom, antall_avstemte_oppdrag) VALUES (?, 'SPREF', ?, 0)"""
        run(queryOf(query, UUID.randomUUID(), avstemmingsnøkkel).asExecute)
    }
}
