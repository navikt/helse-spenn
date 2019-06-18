package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.vedtak.OppdragStateDTO
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class AvstemmingMapperTest {

    private var oppdragIdSequence = 1L
    private var utbetalingsLinjeIdSequence = 1L

    /*@Test
    fun test1() {
        val oppdragsliste = listOf(
                lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1000),
                lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1100)
        )
        val mapper = AvstemmingMapper(oppdragsliste, ØkonomiKodeFagområde.SYKEPENGER_REFUSJON_ARBEIDSGIVER)
        val avstemmingsmeldinger = mapper.lagAvstemmingsMeldinger()

        assertEquals(AksjonType.START, avstemmingsmeldinger[0].aksjon.aksjonType)
        assertEquals(AksjonType.DATA, avstemmingsmeldinger[1].aksjon.aksjonType)
        assertEquals(AksjonType.AVSL, avstemmingsmeldinger[2].aksjon.aksjonType)
    }*/

    @Test
    fun testOpprettDetaljdata() {
        val oppdragsliste = listOf(
                lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1000),
                lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "08", dagSats = 1000),
                lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "04", dagSats = 1100),
                lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "12", dagSats = 1200),
                lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1300),
                lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1400)
        )
        val mapper = AvstemmingMapper(oppdragsliste, ØkonomiKodeFagområde.SYKEPENGER_REFUSJON_ARBEIDSGIVER)

        val detaljer = mapper.opprettDetaljdata()

        assertEquals(3, detaljer.size)

        oppdragsliste.get(1).let { oppdrag ->
            detaljer.get(0).let { detalj ->
                assertEquals("08", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.id.toString(), detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.AVVI, detalj.detaljType)
            }
        }
        oppdragsliste.get(2).let { oppdrag ->
            detaljer.get(1).let { detalj ->
                assertEquals("04", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.id.toString(), detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.VARS, detalj.detaljType)
            }
        }

        oppdragsliste.get(3).let { oppdrag ->
            detaljer.get(2).let { detalj ->
                assertEquals("12", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.id.toString(), detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.AVVI, detalj.detaljType)
            }
        }
    }

    private fun lagOppdragResponseXml(soknadId:String, status: OppdragStateStatus, alvorlighetsgrad: String) : String? {
        if (status == OppdragStateStatus.SENDT_OS) {
            return null
        }
        val kvittering = Oppdrag()
        kvittering.mmel = Mmel()
        kvittering.mmel.kodeMelding = "Melding"
        kvittering.mmel.alvorlighetsgrad = alvorlighetsgrad
        kvittering.mmel.beskrMelding = "Beskrivelse"
        kvittering.oppdrag110 = Oppdrag110()
        kvittering.oppdrag110.fagsystemId = soknadId
        return JAXBOppdrag().fromOppdragToXml(kvittering)
    }

    private fun lagOppdrag(status: OppdragStateStatus = OppdragStateStatus.FERDIG,
                           alvorlighetsgrad: String = "00",
                           dagSats: Long = 1345) : OppdragStateDTO {
        val soknadId = UUID.randomUUID()
        return OppdragStateDTO(
                id = oppdragIdSequence++,
                created = LocalDateTime.now(),
                modified = LocalDateTime.now(),
                oppdragResponse = lagOppdragResponseXml(soknadId.toString(), status, alvorlighetsgrad),
                simuleringResult = null,
                soknadId = soknadId,
                status = status,
                utbetalingsOppdrag = UtbetalingsOppdrag(
                        operasjon = AksjonsKode.OPPDATER,
                        oppdragGjelder = "12121210010",
                        utbetalingsLinje = listOf(UtbetalingsLinje(
                                id = (utbetalingsLinjeIdSequence++).toString(),
                                datoFom = LocalDate.now().minusWeeks(4),
                                datoTom = LocalDate.now().minusWeeks(1),
                                grad = BigInteger.valueOf(100),
                                sats = BigDecimal.valueOf(dagSats),
                                satsTypeKode = SatsTypeKode.DAGLIG,
                                utbetalesTil = "999988887"
                        ))
                )


        )
    }


}