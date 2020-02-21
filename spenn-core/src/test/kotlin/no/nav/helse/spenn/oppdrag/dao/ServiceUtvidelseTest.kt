package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.oppdrag.EndringsKode
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.etUtbetalingsOppdrag
import no.nav.helse.spenn.testsupport.etUtbetalingsUtvidelsesOppdrag
import no.nav.helse.spenn.testsupport.kvittering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class ServiceUtvidelseTest {

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
    fun `fortsettelse av oppdrag`() {

        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val transaksjoner = service.hentNyeOppdrag(5)
        assertEquals(1, transaksjoner.size)
        repository.findAllByStatus(TransaksjonStatus.STARTET).first().apply {
            assertEquals(utbetaling.utbetalingsreferanse, this.utbetalingsreferanse)
        }

        val trans = service.hentNyeOppdrag(5).first()
        trans.forberedSendingTilOS()
        trans.lagreOSResponse(TransaksjonStatus.FERDIG, kvittering, null)
        val dto = repository.findByRef(utbetalingsreferanse = utbetaling.utbetalingsreferanse).first()
        assertEquals(TransaksjonStatus.FERDIG, dto.status)

        val fortsettelse = etUtbetalingsUtvidelsesOppdrag()
        assertEquals(utbetaling.utbetalingsreferanse, fortsettelse.utbetalingsreferanse, "må være samme ref ellers gir ikke testen mening")

        service.lagreNyttOppdrag(fortsettelse)
        val fortsettelsesTrans = service.hentNyeOppdrag(5).first()

        fortsettelsesTrans.simuleringRequest.apply {
            assertEquals(EndringsKode.UENDRET.kode, this.request.oppdrag.kodeEndring)
            assertEquals(1, this.request.oppdrag.oppdragslinje.size)
            assertEquals(EndringsKode.ENDRING.kode, this.request.oppdrag.oppdragslinje.first().kodeEndringLinje)
        }

        fortsettelsesTrans.forberedSendingTilOS()
        fortsettelsesTrans.oppdragRequest.apply {
            assertEquals(EndringsKode.UENDRET.kode, this.oppdrag110.kodeEndring)
            assertEquals(1, this.oppdrag110.oppdragsLinje150.size)
            assertEquals(EndringsKode.ENDRING.kode, this.oppdrag110.oppdragsLinje150.first().kodeEndringLinje)
        }
        fortsettelsesTrans.lagreOSResponse(TransaksjonStatus.FERDIG, kvittering, null)

        // replay feiler:
        assertThrows<SanityCheckException> { service.lagreNyttOppdrag(fortsettelse) }
    }


    @Test
    fun `fortsettelse av oppdrag i status STARTET godtas ikke og gir SanityCheckException`() {
        val utbetaling = etUtbetalingsOppdrag()
        service.lagreNyttOppdrag(utbetaling)
        val transaksjoner = service.hentNyeOppdrag(5)
        assertEquals(1, transaksjoner.size)
        repository.findAllByStatus(TransaksjonStatus.STARTET).first().apply {
            assertEquals(utbetaling.utbetalingsreferanse, this.utbetalingsreferanse)
        }

        val fortsettelse = etUtbetalingsUtvidelsesOppdrag()
        assertThrows<SanityCheckException> {
            service.lagreNyttOppdrag(fortsettelse)
        }
    }
}