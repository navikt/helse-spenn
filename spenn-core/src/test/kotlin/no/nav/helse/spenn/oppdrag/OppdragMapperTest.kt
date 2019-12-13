package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.core.avstemmingsnokkelFormatter
import no.nav.helse.spenn.oppdrag.dao.TransaksjonDTO
import no.nav.helse.spenn.testsupport.etUtbetalingsOppdrag
import no.nav.system.os.entiteter.typer.simpletypes.KodeStatus
import no.trygdeetaten.skjema.oppdrag.TkodeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertNull

class OppdragMapperTest {

    private val maksDato = LocalDate.now().plusYears(1).minusDays(50)
    private val vedtakFom = LocalDate.now().minusWeeks(2)
    private val vedtakTom = LocalDate.now()

    private fun enDTO(): TransaksjonDTO {
        val enOppdragsLinje = UtbetalingsLinje(
            id = "1234567890",
            datoFom = vedtakFom,
            datoTom = vedtakTom,
            sats = BigDecimal.valueOf(1230),
            satsTypeKode = SatsTypeKode.MÅNEDLIG,
            utbetalesTil = "995816598",
            grad = BigInteger.valueOf(100)
        )
        val utbetalingTemplate = etUtbetalingsOppdrag()
        val utbetaling = utbetalingTemplate.copy(
            oppdragGjelder = "12121212345",
            utbetalingsreferanse = "1001",
            utbetaling = utbetalingTemplate.utbetaling!!.copy(
                utbetalingsLinjer = listOf(enOppdragsLinje),
                maksdato = maksDato
            )
        )
        return TransaksjonDTO(
            id = 1L,
            utbetalingsreferanse = utbetaling.utbetalingsreferanse,
            utbetalingsOppdrag = utbetaling,
            nokkel = LocalDateTime.now()
        )
    }

    @Test
    fun `mapping av simuleringsrequest fungerer`() {
        val oppdragState = enDTO()
        val oppdrag = oppdragState.toSimuleringRequest()

        assertEquals("12121212345", oppdrag.request.oppdrag.oppdragGjelderId)
        assertEquals("1001", oppdrag.request.oppdrag.fagsystemId)
        assertEquals(UtbetalingsfrekvensKode.MÅNEDLIG.kode, oppdrag.request.oppdrag.utbetFrekvens)
        assertEquals(FagOmraadekode.SYKEPENGER_REFUSJON.kode, oppdrag.request.oppdrag.kodeFagomraade)

        assertNull(oppdrag.request.oppdrag.oppdragslinje[0].utbetalesTilId)
        assertEquals("00995816598", oppdrag.request.oppdrag.oppdragslinje[0].refusjonsInfo.refunderesId)
        assertEquals(maksDato.toString(), oppdrag.request.oppdrag.oppdragslinje[0].refusjonsInfo.maksDato)
        assertEquals(vedtakFom.toString(), oppdrag.request.oppdrag.oppdragslinje[0].refusjonsInfo.datoFom)
    }

    @Test
    fun `mapping av oppdrag request fungerer`() {
        val oppdragState = enDTO()
        val oppdrag = oppdragState.toOppdragRequest()

        assertEquals(AksjonsKode.OPPDATER.kode, oppdrag.oppdrag110.kodeAksjon)
        assertEquals("12121212345", oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals("1001", oppdrag.oppdrag110.fagsystemId)
        assertEquals(UtbetalingsfrekvensKode.MÅNEDLIG.kode, oppdrag.oppdrag110.utbetFrekvens)
        assertEquals(FagOmraadekode.SYKEPENGER_REFUSJON.kode, oppdrag.oppdrag110.kodeFagomraade)

        assertEquals(
            avstemmingsnokkelFormatter.format(oppdragState.nokkel),
            oppdrag.oppdrag110.avstemming115.nokkelAvstemming
        )
        assertNull(oppdrag.oppdrag110.oppdragsLinje150[0].utbetalesTilId)
        assertEquals("00995816598", oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.refunderesId)
        assertEquals(
            DatatypeFactory.newInstance().newXMLGregorianCalendar(
                GregorianCalendar.from(
                    maksDato.atStartOfDay(ZoneId.systemDefault())
                )
            ),
            oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.maksDato
        )
        assertEquals(
            DatatypeFactory.newInstance().newXMLGregorianCalendar(
                GregorianCalendar.from(
                    vedtakFom.atStartOfDay(ZoneId.systemDefault())
                )
            ),
            oppdrag.oppdrag110.oppdragsLinje150[0].refusjonsinfo156.datoFom
        )
    }

    @Test
    fun `mapping av annulleringsrequest fungerer`() {
        val annulleringsDTO = enDTO().let {
            it.copy(
                utbetalingsOppdrag = it.utbetalingsOppdrag.copy(
                    utbetaling = null,
                    statusEndringFom = vedtakFom
                )
            )
        }
        val oppdrag = annulleringsDTO.toOppdragRequest()

        assertEquals(FagOmraadekode.SYKEPENGER_REFUSJON.kode, oppdrag.oppdrag110.kodeFagomraade)
        assertEquals(AksjonsKode.OPPDATER.kode, oppdrag.oppdrag110.kodeAksjon)
        assertEquals(EndringsKode.ENDRING.kode, oppdrag.oppdrag110.kodeEndring)
        assertEquals(TkodeStatus.OPPH, oppdrag.oppdrag110.kodeStatus)
        assertEquals(
            OppdragSkjemaConstants.toXMLDate(annulleringsDTO.utbetalingsOppdrag.statusEndringFom!!),
            oppdrag.oppdrag110.datoStatusFom
        )
        assertEquals("12121212345", oppdrag.oppdrag110.oppdragGjelderId)
        assertEquals("1001", oppdrag.oppdrag110.fagsystemId)
        assertEquals(
            avstemmingsnokkelFormatter.format(annulleringsDTO.nokkel),
            oppdrag.oppdrag110.avstemming115.nokkelAvstemming
        )
        assertEquals(UtbetalingsfrekvensKode.MÅNEDLIG.kode, oppdrag.oppdrag110.utbetFrekvens)
        assertEquals(FagOmraadekode.SYKEPENGER_REFUSJON.kode, oppdrag.oppdrag110.kodeFagomraade)
        assertTrue(oppdrag.oppdrag110.oppdragsLinje150.isEmpty())
    }

    @Test
    fun `mapping av annulleringssimuleringsrequest`() {
        val annulleringsDTO = enDTO().let {
            it.copy(
                utbetalingsOppdrag = it.utbetalingsOppdrag.copy(
                    utbetaling = null,
                    statusEndringFom = vedtakFom
                )
            )
        }
        val oppdrag = annulleringsDTO.toSimuleringRequest()

        assertEquals(EndringsKode.ENDRING.kode, oppdrag.request.oppdrag.kodeEndring)
        assertEquals(KodeStatus.OPPH, oppdrag.request.oppdrag.kodeStatus)
        assertEquals(
            annulleringsDTO.utbetalingsOppdrag.statusEndringFom!!.toString(),
            oppdrag.request.oppdrag.datoStatusFom
        )

        assertEquals("12121212345", oppdrag.request.oppdrag.oppdragGjelderId)
        assertEquals("1001", oppdrag.request.oppdrag.fagsystemId)
        assertEquals(UtbetalingsfrekvensKode.MÅNEDLIG.kode, oppdrag.request.oppdrag.utbetFrekvens)
        assertEquals(FagOmraadekode.SYKEPENGER_REFUSJON.kode, oppdrag.request.oppdrag.kodeFagomraade)

        assertTrue(oppdrag.request.oppdrag.oppdragslinje.isEmpty())
    }
    @Test
    fun `mapping skal feile hvis nøkkel ikke er satt`() {
        assertThrows<KotlinNullPointerException> { enDTO().copy(nokkel = null).toOppdragRequest() }
    }
}


