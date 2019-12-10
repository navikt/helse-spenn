package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.TestData.Companion.etUtbetalingsOppdrag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ServiceTest {

    private lateinit var repository: TransaksjonRepository
    private lateinit var service: OppdragService

    @BeforeEach
    fun setup() {
        val dataSource = TestDb.createMigratedDataSource()
        dataSource.connection.use { connection ->
            connection.prepareStatement("delete from transaksjon").executeUpdate()
            connection.prepareStatement("delete from oppdrag").executeUpdate()
        }
        repository = TransaksjonRepository(dataSource)
        service = OppdragService(dataSource)
    }

    @Test
    fun `lagre utbetalingsoppdrag`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val transaksjoner = service.hentNyeBehov(5)
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

    @Test
    fun `test stopp oppdrag`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeBehov(5).first()
        trans.stopp("neineinei")
        val stoppede = repository.findAllByStatus(TransaksjonStatus.STOPPET)
        assertEquals(1, stoppede.size)
        assertEquals(utbetaling.behov.utbetalingsreferanse, stoppede.first().utbetalingsreferanse)
    }

    @Test
    fun `forbered sending til OS`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val ny = repository.findByRef(utbetaling.behov.utbetalingsreferanse).first()
        assertNull(ny.nokkel)

        service.hentNyeBehov(5).first().forberedSendingTilOS()

        val klarForOS = repository.findByRef(utbetaling.behov.utbetalingsreferanse).first()
        assertNotNull(klarForOS.nokkel)
        assertEquals(TransaksjonStatus.SENDT_OS, klarForOS.status)
        assertTrue(klarForOS.nokkel > LocalDateTime.now().minusSeconds(10))
        assertTrue(klarForOS.nokkel < LocalDateTime.now().plusSeconds(10))
    }

    @Test
    fun `sanity check`() {
        val utbetaling = etUtbetalingsOppdrag()
        val utbetalingMedForHøyDagsats = utbetaling.copy(utbetalingsLinje = listOf(utbetaling.utbetalingsLinje.first().copy(
            sats = BigDecimal.valueOf(3000))))
        println(utbetalingMedForHøyDagsats)
        service.lagreNyttOppdrag(utbetalingMedForHøyDagsats)
        assertThrows<SanityCheckException> {
            service.hentNyeBehov(5).first().forberedSendingTilOS()
        }
    }

    @Test
    fun `lagre simuleringsresultat OK`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val result = SimuleringResult(status = SimuleringStatus.OK)
        service.hentNyeBehov(5).first()
            .oppdaterSimuleringsresultat(result)
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.behov.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.SIMULERING_OK, dto.status)
        assertEquals(defaultObjectMapper.writeValueAsString(result), dto.simuleringresult)
    }

    @Test
    fun `lagre simuleringsresultat FEIL`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        service.hentNyeBehov(5).first()
            .oppdaterSimuleringsresultat(SimuleringResult(status = SimuleringStatus.FEIL))
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.behov.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.SIMULERING_FEIL, dto.status)
    }


}