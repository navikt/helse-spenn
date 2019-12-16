package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.etUtbetalingsOppdrag
import no.nav.helse.spenn.testsupport.kvittering
import no.nav.helse.spenn.testsupport.simuleringsresultat
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
        val transaksjoner = service.hentNyeOppdrag(5)
        assertEquals(1, transaksjoner.size)
        repository.findAllByStatus(TransaksjonStatus.STARTET).first().apply {
            assertEquals(utbetaling.utbetalingsreferanse, this.utbetalingsreferanse)
        }
    }

    @Test
    fun `annuler utbetaling`() {
        val utbetaling = etUtbetalingsOppdrag()
        val annulering = utbetaling.copy(utbetaling = null)
        service.lagreNyttOppdrag(utbetaling)
        service.annulerUtbetaling(annulering)
        val transaksjoner = repository.findByRef(utbetaling.utbetalingsreferanse)
        assertEquals(2, transaksjoner.size)
        assertEquals(utbetaling.utbetalingsreferanse, transaksjoner.first().utbetalingsreferanse)
        assertEquals(utbetaling.utbetalingsreferanse, transaksjoner.last().utbetalingsreferanse)

        println(transaksjoner.last().utbetalingsOppdrag)
        assertEquals(utbetaling.utbetaling!!.utbetalingsLinjer.first().datoFom,
            transaksjoner.last().utbetalingsOppdrag.statusEndringFom)
    }

    @Test
    fun `lagre annuleringssimuleringsresultat`() {
        val utbetaling = etUtbetalingsOppdrag()
        val annulering = utbetaling.copy(utbetaling = null)
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeOppdrag(5).first()
        trans.forberedSendingTilOS()
        trans.lagreOSResponse(TransaksjonStatus.FERDIG, kvittering, null)

        service.annulerUtbetaling(annulering)
        val annulleringTrans = service.hentNyeOppdrag(5).first()
        annulleringTrans.oppdaterSimuleringsresultat(SimuleringResult(
            status = SimuleringStatus.OK,
            simulering = null
        ))

        assertEquals(TransaksjonStatus.SIMULERING_OK, annulleringTrans.dto.status)
    }

    @Test
    fun `ikke godta tomt simuleringsresultat for annet enn annulering`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeOppdrag(5).first()

        assertThrows<IllegalArgumentException> {
            trans.oppdaterSimuleringsresultat(
                SimuleringResult(
                    status = SimuleringStatus.OK,
                    simulering = null
                )
            )
        }
    }

    @Test
    fun `test stopp oppdrag`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeOppdrag(5).first()
        trans.stopp("neineinei")
        val stoppede = repository.findAllByStatus(TransaksjonStatus.STOPPET)
        assertEquals(1, stoppede.size)
        assertEquals(utbetaling.utbetalingsreferanse, stoppede.first().utbetalingsreferanse)
    }

    @Test
    fun `forbered sending til OS`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val ny = repository.findByRef(utbetaling.utbetalingsreferanse).first()
        assertNull(ny.nokkel)

        service.hentNyeOppdrag(5).first().forberedSendingTilOS()

        val klarForOS = repository.findByRef(utbetaling.utbetalingsreferanse).first()
        assertNotNull(klarForOS.nokkel)
        assertEquals(TransaksjonStatus.SENDT_OS, klarForOS.status)
        assertTrue(klarForOS.nokkel > LocalDateTime.now().minusSeconds(10))
        assertTrue(klarForOS.nokkel < LocalDateTime.now().plusSeconds(10))
    }

    @Test
    fun `sanity check`() {
        val utbetaling = etUtbetalingsOppdrag()
        val utbetalingMedForHøyDagsats = utbetaling.copy(
            utbetaling = utbetaling.utbetaling!!.copy(
            utbetalingsLinjer = listOf(utbetaling.utbetaling!!.utbetalingsLinjer.first().copy(
            sats = BigDecimal.valueOf(3000)))))
        println(utbetalingMedForHøyDagsats)
        service.lagreNyttOppdrag(utbetalingMedForHøyDagsats)
        assertThrows<SanityCheckException> {
            service.hentNyeOppdrag(5).first().forberedSendingTilOS()
        }
    }

    @Test
    fun `lagre simuleringsresultat OK`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val result = simuleringsresultat
        service.hentNyeOppdrag(5).first().oppdaterSimuleringsresultat(result)
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.SIMULERING_OK, dto.status)
        assertEquals(result,
            defaultObjectMapper.readValue(dto.simuleringresult, SimuleringResult::class.java))
    }

    @Test
    fun `lagre simuleringsresultat FEIL`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        service.hentNyeOppdrag(5).first()
            .oppdaterSimuleringsresultat(SimuleringResult(status = SimuleringStatus.FEIL))
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.SIMULERING_FEIL, dto.status)
    }

    @Test
    fun `lagre os-respons`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeOppdrag(5).first()
        trans.forberedSendingTilOS()
        trans.lagreOSResponse(TransaksjonStatus.FERDIG, kvittering, null)
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.FERDIG, dto.status)
    }

    @Test
    fun `sett til avstemt`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val trans = service.hentNyeOppdrag(5).first()
        trans.markerSomAvstemt()
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertTrue(dto.avstemt)
    }

    @Test
    fun `behov skal ikke parses, men forbli inntakt`() {
        val etBehov = """{"heidu":"jauda æøå !! 123","denada":123.44,"apropo":{"a":123,"æøå":"græit"}}"""
        val utbetaling = etUtbetalingsOppdrag().copy(behov = defaultObjectMapper.readTree(etBehov))
        service.lagreNyttOppdrag(utbetaling)
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertEquals(etBehov, dto.utbetalingsOppdrag.behov.toString())
    }
}
