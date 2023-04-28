package no.nav.helse.spenn.avstemming.e2e

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.avstemming.Database
import no.nav.helse.spenn.avstemming.Oppdragstatus
import no.nav.helse.spenn.avstemming.UtKø
import no.nav.helse.spenn.avstemming.rapidApp
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class E2ETest {
    private companion object {
        private val database = TestDatabase()
    }
    private val testRapid = RepublishableTestRapid()
    private val mapper = jacksonObjectMapper()
    private val mqmeldinger = mutableListOf<String>()
    private val utkø = object : UtKø {
        override fun send(messageString: String) {
            mqmeldinger.add(messageString)
        }
    }

    init {
        rapidApp(testRapid, database, utkø)
    }

    @AfterEach
    fun after() {
        mqmeldinger.clear()
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

    @Test
    fun `avstemmer oppdrag`() {
        val avstemmingsnøkkel = 1024L
        val avstemmingsnøkkel2 = 1023L
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

        testRapid.sendTestMessage(oppdragutbetaling(avstemmingsnøkkel2, UUID.randomUUID(), "annen fagsystemId", fagområde, fødselsnummer, mottaker, totalbeløp, opprettet))
        assertEquals(2, database.antallOppdrag())

        assertEquals(0, testRapid.inspektør.size)
        testRapid.sendTestMessage(utførAvstemming(opprettet.toLocalDate()))
        assertEquals(0, testRapid.inspektør.size)
        assertEquals(0, mqmeldinger.size)

        testRapid.sendTestMessage(oppdragutbetaling.medKvittering("OVERFØRT"))
        testRapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, "00", "AKSEPTERT", null, null, "xml"))

        assertEquals(0, testRapid.inspektør.size)
        testRapid.sendTestMessage(utførAvstemming(opprettet.toLocalDate()))
        assertEquals(1, testRapid.inspektør.size)
        assertEquals(3, mqmeldinger.size)
        val avstemming = testRapid.inspektør.message(0)

        assertEquals("avstemming", avstemming.path("@event_name").asText())
        assertEquals("SPREF", avstemming.path("fagområde").asText())
        assertEquals(1, avstemming.path("detaljer").path("antall_oppdrag").asInt())

        testRapid.sendTestMessage(utførAvstemming(opprettet.toLocalDate()))
        assertEquals(1, testRapid.inspektør.size) { "skal ikke avstemme allerede avstemte oppdrag" }
        assertEquals(3, mqmeldinger.size)

        assertTrue(database.avstemt(avstemmingsnøkkel))
        assertFalse(database.avstemt(avstemmingsnøkkel2))
    }

    @Language("JSON")
    private fun utførAvstemming(dagen: LocalDate) = """
        {
          "@event_name": "utfør_avstemming",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "dagen": "$dagen"
        }
    """

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
          "utbetalingId": "${UUID.randomUUID()}",
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
            initializationFailTimeout = 10_000L
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

        fun avstemt(avstemmingsnøkkel: Long) = sessionOf(getDataSource()).use {
            it.run(queryOf("SELECT avstemt FROM oppdrag WHERE avstemmingsnokkel=?", avstemmingsnøkkel).map { it.boolean(1) }.asSingle)
        } ?: fail { "forventer å finne oppdrag" }
    }

    internal class RepublishableTestRapid(private val rapid: TestRapid = TestRapid()) : RapidsConnection(), RapidsConnection.StatusListener, RapidsConnection.MessageListener {
        val inspektør get() = rapid.inspektør

        init {
            rapid.register(this as StatusListener)
            rapid.register(this as MessageListener)
        }

        fun reset() = rapid.reset()
        override fun onMessage(message: String, context: MessageContext) = notifyMessage(message, context)
        override fun onNotReady(rapidsConnection: RapidsConnection) = notifyNotReady()
        override fun onReady(rapidsConnection: RapidsConnection) = notifyReady()
        override fun onShutdown(rapidsConnection: RapidsConnection) = notifyShutdown()
        override fun onStartup(rapidsConnection: RapidsConnection) = notifyStartup()

        fun sendTestMessage(message: String) = notifyMessage(message, this)

        override fun publish(message: String) {
            rapid.publish(message)
            rapid.sendTestMessage(message)
        }

        override fun publish(key: String, message: String) {
            rapid.publish(key, message)
            rapid.sendTestMessage(message)
        }

        override fun rapidName() = rapid.rapidName()
        override fun start() = rapid.start()
        override fun stop() = rapid.start()
    }
}