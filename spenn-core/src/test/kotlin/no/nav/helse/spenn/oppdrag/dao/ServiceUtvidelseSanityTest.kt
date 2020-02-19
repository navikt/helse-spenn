package no.nav.helse.spenn.oppdrag.dao

import junit.framework.Assert.assertTrue
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.Utbetaling
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import no.nav.helse.spenn.testsupport.TestDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime

internal class ServiceUtvidelseSanityTest {

    private lateinit var repository: TransaksjonRepository
    private lateinit var service: OppdragService

    private val orgNr = "123456789"
    private val fnr = "01010112345"

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
    fun `utvidelse sanity check bestås for OK utvidelse`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = listOf(okLinje2)
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
    }

    @Test
    fun `skal feile på annen dagsats`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = listOf(okLinje2.copy(sats = okLinje1.sats.plus(1.toBigDecimal())))
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har ulik dagsats"))
    }

    @Test
    fun `utvidelse er ikke greit når forrige trans ikke er FERDIG`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = listOf(okLinje2)
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.SIMULERING_OK))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har status=SIMULERING_OK"))
    }

    @Test
    fun `utvidelse må ha FOM lik FOM på gammel linje`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer =  listOf(okLinje2.copy(datoFom = okLinje1.datoFom.plusDays(1)))
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har annen 'datoFom'"))
    }

    @Test
    fun `utvidelse må ha TOM etter TOM på gammel linje`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer =  listOf(okLinje2.copy(datoTom = okLinje1.datoTom))
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har ikke 'datoTom' etter forrige datoTom"))
    }

    @Test
    fun `utvidelse må ha samme 'oppdragGjelder`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = listOf(okLinje2)
                ),
                oppdragGjelder = "01019012345"
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har annen 'oppdragGjelder'"))
    }

    @Test
    fun `utvidelse må ha samme organisasjonsnummer`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        organisasjonsnummer = "999888777",
                        utbetalingsLinjer = listOf(okLinje2)
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        val ex = assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
        assertTrue(ex.message!!.contains("har organisasjonsnummer forskjellig fra utvidelse"))
    }


    private val okLinje1 = UtbetalingsLinje(
            id = "1",
            grad = BigInteger.valueOf(100),
            datoFom = LocalDate.of(2011, 1, 1),
            datoTom = LocalDate.of(2011, 1, 31),
            utbetalesTil = orgNr,
            sats = BigDecimal.valueOf(1000.0),
            satsTypeKode = SatsTypeKode.DAGLIG
    )
    private val okLinje2 = UtbetalingsLinje(
            id = "1",
            grad = BigInteger.valueOf(100),
            datoFom = LocalDate.of(2011, 1, 1),
            datoTom = LocalDate.of(2011, 2, 28),
            utbetalesTil = orgNr,
            sats = BigDecimal.valueOf(1000.0),
            satsTypeKode = SatsTypeKode.DAGLIG)


    private fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
        return UtbetalingsOppdrag(
                behov = """{ "somejson" : 123 }""",
                utbetalingsreferanse = "ref1",
                oppdragGjelder = fnr,
                saksbehandler = "Z999999",
                utbetaling = Utbetaling(
                        maksdato = LocalDate.of(2011, 12, 20),
                        organisasjonsnummer = orgNr,
                        utbetalingsLinjer = listOf(okLinje1)
                )
        )
    }
}


private fun UtbetalingsOppdrag.tilDTO(status:TransaksjonStatus = TransaksjonStatus.FERDIG, created:LocalDateTime = LocalDateTime.now()) : TransaksjonDTO {
    return TransaksjonDTO(
            id = -1,
            utbetalingsreferanse = this.utbetalingsreferanse,
            status = status,
            utbetalingsOppdrag = this,
            created = created
    )
}
