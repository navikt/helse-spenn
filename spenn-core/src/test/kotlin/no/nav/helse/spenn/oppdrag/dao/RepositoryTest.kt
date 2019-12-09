package no.nav.helse.spenn.oppdrag.dao

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RepositoryTest {

    lateinit var repository: OppdragStateRepository

    @BeforeEach
    fun setup() {
        val dataSource = TestDb.createMigratedDataSource()
        dataSource.connection.use {
            it.prepareStatement("delete from transaksjon").executeUpdate()
            it.prepareStatement("delete from oppdrag").executeUpdate()
        }
        repository = OppdragStateRepository(dataSource)
    }

    @Test
    fun `opprett nye oppdrag`() {
        val oppdrag = etUtbetalingsOppdrag()
        repository.insertNyttOppdrag(oppdrag)
        val res = repository.findAllByStatus(TransaksjonStatus.STARTET)
        assertEquals(1, res.size)
        res.first().apply {
            assertEquals(UUID.fromString("e25ccad5-f5d5-4399-bb9d-43e9fc487888"), this.sakskompleksId)
            assertEquals("1", this.utbetalingsreferanse)
            assertNull(this.nokkel)
            assertEquals(oppdrag, this.utbetalingsOppdrag)
        }
    }

    @Test
    fun `nytt oppdrag med samme referanse skal feile`() {
        val oppdrag = etUtbetalingsOppdrag()
        repository.insertNyttOppdrag(oppdrag)
        assertThrows<SQLIntegrityConstraintViolationException> {
            repository.insertNyttOppdrag(oppdrag)
        }
    }

    @Test
    fun `opprett annulering`() {
        val utbetaling = etUtbetalingsOppdrag()
        val annulering = utbetaling.copy(annulering = true)
        repository.insertNyttOppdrag(utbetaling)
        repository.insertNyTransaksjon(annulering)
        val res = repository.findAllByStatus(TransaksjonStatus.STARTET)
        println(res)
        assertEquals(2, res.size)
        res.last().apply {
            assertTrue { this.utbetalingsOppdrag.annulering }
        }
    }

    private fun etUtbetalingsOppdrag() : UtbetalingsOppdrag {
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        return behov.tilUtbetaling("01010112345")
    }

}