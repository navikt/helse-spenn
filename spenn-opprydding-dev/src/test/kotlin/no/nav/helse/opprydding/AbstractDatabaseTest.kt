package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.util.*

val databaseContainer = DatabaseContainers.container("spenn-opprydding", CleanupStrategy.tables("oppdrag,avstemming"))

internal abstract class AbstractDatabaseTest {

    protected lateinit var personRepository: PersonRepository
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        personRepository = PersonRepository(dataSource.ds)
    }

    @AfterEach
    fun teardown() {
        // gi tilbake tilkoblingen
        databaseContainer.droppTilkobling(dataSource)
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
        return sessionOf(dataSource.ds).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            session.run(queryOf(query).map { it.string("table_name") }.asList)
        }
    }

    private fun finnTabellCount(tabellnavn: String): Int {
        return sessionOf(dataSource.ds).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM $tabellnavn"
            session.run(queryOf(query).map { it.int(1) }.asSingle) ?: 0
        }
    }

    protected fun opprettPerson(fødselsnummer: String, avstemmingsnøkkel: Int) {
        sessionOf(dataSource.ds).use { session ->
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
