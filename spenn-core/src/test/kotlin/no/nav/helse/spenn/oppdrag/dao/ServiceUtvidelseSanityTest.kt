package no.nav.helse.spenn.oppdrag.dao

import no.nav.helse.spenn.core.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.*
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
                        utbetalingsLinjer = opprinnelig.utbetaling!!.utbetalingsLinjer + okLinje2
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
    }

    @Test
    fun `utvidelse må inneholde alle eksisterende linjer`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = listOf(okLinje2)
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
    }

    @Test
    fun `utvidelse er ikke greit når forrige trans ikke er FERDIG`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = opprinnelig.utbetaling!!.utbetalingsLinjer + okLinje2
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.SIMULERING_OK))
        assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
    }

    @Test
    fun `utvidelse må ha FOM etter TOM på gamle linjer`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer =  opprinnelig.utbetaling!!.utbetalingsLinjer + okLinje2.copy(
                                datoFom = okLinje1.datoTom
                        )
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
    }

    @Test
    fun `utvidelse må ha samme 'oppdragGjelder`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        utbetalingsLinjer = opprinnelig.utbetaling!!.utbetalingsLinjer + okLinje2
                ),
                oppdragGjelder = "01019012345"
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
    }

    @Test
    fun `utvidelse må ha samme organisasjonsnummer`() {
        val opprinnelig = etUtbetalingsOppdrag()
        val utvidelse = opprinnelig.copy(
                utbetaling = opprinnelig.utbetaling!!.copy(
                        organisasjonsnummer = "999888777",
                        utbetalingsLinjer = opprinnelig.utbetaling!!.utbetalingsLinjer + okLinje2
                )
        )
        val eksisterendeTranser = listOf(opprinnelig.tilDTO(TransaksjonStatus.FERDIG))
        assertThrows<SanityCheckException> {
            OppdragService.sanityCheckUtvidelse(eksisterendeTranser, utvidelse)
        }
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
            id = "2",
            grad = BigInteger.valueOf(100),
            datoFom = LocalDate.of(2011, 2, 1),
            datoTom = LocalDate.of(2011, 2, 28),
            utbetalesTil = orgNr,
            sats = BigDecimal.valueOf(1000.0),
            satsTypeKode = SatsTypeKode.DAGLIG)


    private fun etUtbetalingsOppdrag(): UtbetalingsOppdrag {
        return UtbetalingsOppdrag(
                behov = defaultObjectMapper.readTree("""{ "somejson" : 123 }"""),
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