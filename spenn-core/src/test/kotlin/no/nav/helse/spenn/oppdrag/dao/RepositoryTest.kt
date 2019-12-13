package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.etUtbetalingsOppdrag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RepositoryTest {

    private lateinit var repository: TransaksjonRepository

    @BeforeEach
    fun setup() {
        val dataSource = TestDb.createMigratedDataSource()
        dataSource.connection.use { connection ->
            connection.prepareStatement("delete from transaksjon").executeUpdate()
            connection.prepareStatement("delete from oppdrag").executeUpdate()
        }
        repository = TransaksjonRepository(dataSource)
    }

    @Test
    fun `opprett nye oppdrag`() {
        val oppdrag = etUtbetalingsOppdrag()
        repository.insertNyttOppdrag(oppdrag)
        val res = repository.findAllByStatus(TransaksjonStatus.STARTET)
        assertEquals(1, res.size)
        res.first().apply {
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
        val annulering = utbetaling.copy(utbetaling = null)
        repository.insertNyttOppdrag(utbetaling)
        repository.insertNyTransaksjon(annulering)
        val res = repository.findAllByStatus(TransaksjonStatus.STARTET)
        assertEquals(2, res.size)
        res.last().apply {
            assertNull(this.utbetalingsOppdrag.utbetaling)
        }
    }

}