package no.nav.helse.spenn.avstemming.e2e

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.avstemming.Database
import no.nav.helse.spenn.avstemming.Oppdragstatus
import no.nav.helse.spenn.avstemming.UtKø
import no.nav.helse.spenn.avstemming.rapidApp
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class E2ETest {

    private val testRapid = TestRapid()
    private val database = TestDatabase()
    private val mapper = jacksonObjectMapper()
    private val utkø = object : UtKø {
        override fun send(messageString: String) {}
    }

    init {
        rapidApp(testRapid, database, utkø)
    }

    @AfterEach
    fun after() {
        testRapid.reset()
        database.resetDatabase()
    }

    @Test
    fun `registrerer oppdrag`() {
        val avstemmingsnøkkel = 1024L
        val utbetalingId = UUID.randomUUID()
        val fagsystemId = "asdfg"
        val fagområde = "SPREF"
        val fødselsnummer = "fnr"
        val mottaker = "mottaker"
        val totalbeløp = 5000
        val opprettet = LocalDateTime.now()

        val oppdragutbetaling = oppdragutbetaling(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet)
        testRapid.sendTestMessage(oppdragutbetaling)
        assertEquals(1, database.antallOppdrag())
        assertNull(database.status(avstemmingsnøkkel))

        testRapid.sendTestMessage(oppdragutbetaling.medKvittering("OVERFØRT"))
        assertEquals(1, database.antallOppdrag())
        assertEquals(Oppdragstatus.MANGELFULL, database.status(avstemmingsnøkkel))

        testRapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, "00", "AKSEPTERT", null, null, "xml"))
        assertEquals(1, database.antallOppdrag())
        assertEquals(Oppdragstatus.AKSEPTERT, database.status(avstemmingsnøkkel))
    }

    @Language("JSON")
    private fun oppdragutbetaling(avstemmingsnøkkel: Long, utbetalingId: UUID, fagsystemId: String, fagområde: String, fødselsnummer: String, mottaker: String, totalbeløp: Int, opprettet: LocalDateTime) = """
    {
      "@event_name": "oppdrag_utbetaling",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "$fødselsnummer",
      "aktørId": "aktør",
      "utbetalingId": "$utbetalingId",
      "fagsystemId": "$fagsystemId",
      "fagområde": "$fagområde",
      "mottaker": "$mottaker",
      "opprettet": "$opprettet",
      "avstemmingsnøkkel": $avstemmingsnøkkel,
      "totalbeløp": $totalbeløp
    }
    """


    @Language("JSON")
    private fun String.medKvittering(status: String) = (mapper.readTree(this) as ObjectNode).apply {
        putObject("kvittering").apply {
            put("status", status)
        }
    }.toString()

    @Language("JSON")
    private fun transaksjonStatus(
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        fagsystemId: String,
        alvorlighetsgrad: String,
        status: String,
        kodemelding: String?,
        beskrivendemelding: String?,
        oppdragkvittering: String
    ) = """
        {
          "@event_name": "transaksjon_status",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "fødselsnummer": "$fødselsnummer",
          "avstemmingsnøkkel": $avstemmingsnøkkel,
          "fagsystemId": "$fagsystemId",
          "feilkode_oppdrag": "$alvorlighetsgrad",
          "status": "$status",
          "kodemelding": ${kodemelding?.let { "\"$it\"" } ?: "null"},
          "beskrivendemelding": ${beskrivendemelding?.let { "\"$it\"" } ?: "null"},
          "originalXml": "$oppdragkvittering"
        }
    """

    private class TestDatabase : Database {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:13").also { it.start() }
        private val hikariConfig = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }

        private var actualDataSource: DataSource? = null

        override fun getDataSource(): DataSource {
            return actualDataSource ?: HikariDataSource(hikariConfig).also {
                actualDataSource = it
                migrate()
            }
        }

        override fun migrate() {
            Flyway
                .configure()
                .dataSource(getDataSource())
                .load().also(Flyway::migrate)
        }

        fun resetDatabase() {
            sessionOf(getDataSource()).use {
                it.run(queryOf("TRUNCATE avstemming CASCADE").asExecute)
                it.run(queryOf("TRUNCATE oppdrag CASCADE").asExecute)
            }
        }

        fun antallOppdrag() = sessionOf(getDataSource()).use {
            it.run(queryOf("SELECT COUNT(1) FROM oppdrag").map { it.int(1) }.asSingle)
        } ?: 0

        fun status(avstemmingsnøkkel: Long) = sessionOf(getDataSource()).use {
            it.run(queryOf("SELECT status FROM oppdrag WHERE avstemmingsnokkel=?", avstemmingsnøkkel).map { it.stringOrNull("status")?.let { Oppdragstatus.valueOf(it) } }.asSingle)
        }
    }
}