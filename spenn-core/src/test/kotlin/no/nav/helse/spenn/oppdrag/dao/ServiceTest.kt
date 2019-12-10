package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.TestData.Companion.etUtbetalingsOppdrag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ServiceTest {

    private lateinit var repository: TransaksjonRepository
    private lateinit var service: TransaksjonService

    @BeforeEach
    fun setup() {
        val dataSource = TestDb.createMigratedDataSource()
        dataSource.connection.use { connection ->
            connection.prepareStatement("delete from transaksjon").executeUpdate()
            connection.prepareStatement("delete from oppdrag").executeUpdate()
        }
        repository = TransaksjonRepository(dataSource)
        service = TransaksjonService(repository)
    }

    @Test
    fun `lagre utbetalingsoppdrag`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val transaksjoner = service.hentNyeBehov()
        assertEquals(1, transaksjoner.size)
        repository.findAllByStatus(TransaksjonStatus.STARTET).first().apply {
            assertEquals(utbetaling.behov.utbetalingsreferanse, this.utbetalingsreferanse)
            assertEquals(utbetaling.behov.sakskompleksId, this.sakskompleksId)
        }
    }

    @Test
    fun `annuler utbetaling`() {
        val utbetaling = etUtbetalingsOppdrag()
        val annulering = utbetaling.copy(annulering = true)
        service.lagreNyttOppdrag(utbetaling)
        service.annulerUtbetaling(annulering)
        val transaksjoner = repository.findByRef(utbetaling.behov.utbetalingsreferanse)
        assertEquals(2, transaksjoner.size)
        assertEquals(utbetaling.behov.utbetalingsreferanse, transaksjoner.first().utbetalingsreferanse)
        assertEquals(utbetaling.behov.utbetalingsreferanse, transaksjoner.last().utbetalingsreferanse)
    }

}