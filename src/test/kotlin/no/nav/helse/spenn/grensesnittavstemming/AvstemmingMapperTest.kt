package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.*
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.*
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals

class AvstemmingMapperTest {
    // TODO Roy will fix this
    private var oppdragIdSequence = 1L
    private var utbetalingsLinjeIdSequence = 1L

    private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS")
    private val testoppdragsliste1 = listOf(
            lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1000),
            lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "08", dagSats = 1000),
            lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "04", dagSats = 1100),
            lagOppdrag(status = OppdragStateStatus.FEIL, alvorlighetsgrad = "12", dagSats = 1200),
            lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1300),
            lagOppdrag(status = OppdragStateStatus.FERDIG, dagSats = 1400),
            lagOppdrag(status = OppdragStateStatus.SENDT_OS, dagSats = 1550),
            lagOppdrag(status = OppdragStateStatus.SENDT_OS, dagSats = 1660)
    )

    @Test
    fun testAvstemmingsXml() {
        val oppdragsliste = testoppdragsliste1

        val sjekkAksjon = fun(aksjon: Aksjonsdata, expectedType: AksjonType) {
            assertEquals(expectedType, aksjon.aksjonType)
            assertEquals(KildeType.AVLEV, aksjon.kildeType)
            assertEquals(AvstemmingType.GRSN, aksjon.avstemmingType)
            assertEquals("SP", aksjon.avleverendeKomponentKode)
            assertEquals("OS", aksjon.mottakendeKomponentKode)
            assertEquals("SPREF", aksjon.underkomponentKode)
            assertEquals(oppdragsliste.map { "${it.avstemming!!.nokkel}" }.min(), aksjon.nokkelFom)
            assertEquals(oppdragsliste.map { "${it.avstemming!!.nokkel}" }.max(), aksjon.nokkelTom)
            assertEquals(oppdragsliste.map { it.modified.format(tidspunktFormatter) }.max(), aksjon.tidspunktAvstemmingTom)
            assertEquals("SPA", aksjon.brukerId)
        }

        val mapper = AvstemmingMapper(oppdragsliste, ØkonomiKodeFagområde.SYKEPENGER_REFUSJON_ARBEIDSGIVER)
        val xmlMeldinger = mapper.lagXmlMeldinger()
        //println(xmlMeldinger)
        val meldinger = xmlMeldinger.map { JAXBAvstemmingsdata().toAvstemmingsdata(it) }
        assertEquals(3, meldinger.size)
        sjekkAksjon(meldinger.first().aksjon, AksjonType.START)
        sjekkAksjon(meldinger.last().aksjon, AksjonType.AVSL)

        meldinger[1].let {
            sjekkAksjon(it.aksjon, AksjonType.DATA)
            sjekkDetaljerForTestoppdragsliste1(it.detalj)
            it.total.let {
                assertEquals(Fortegn.T, it.fortegn)
                assertEquals(oppdragsliste.size, it.totalAntall)
                assertEquals(oppdragsliste.map {
                    it.utbetalingsOppdrag.utbetalingsLinje.map {
                        it.sats.toLong()
                    }.sum()
                }.sum(), it.totalBelop.toLong())
            }
            it.periode.let {
                assertEquals(oppdragsliste.map { it.created }.min()!!.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
                        it.datoAvstemtFom)
                assertEquals(oppdragsliste.map { it.created }.max()!!.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
                        it.datoAvstemtTom)
            }
            it.grunnlag.let {
                val ferdige = oppdragsliste.filter { it.status == OppdragStateStatus.FERDIG }
                assertEquals(ferdige.map { satsSum(it)}.sum(), it.godkjentBelop.toLong())
                assertEquals(ferdige.size, it.godkjentAntall)
                assertEquals(Fortegn.T, it.godkjentFortegn)

                val avviste = oppdragsliste.filter {
                    it.status == OppdragStateStatus.FEIL && (getKvitteringsMelding(it)!!.mmel.alvorlighetsgrad in listOf("08", "12"))
                }
                assertEquals(avviste.map { satsSum(it) }.sum(), it.avvistBelop.toLong())
                assertEquals(avviste.size, it.avvistAntall)
                assertEquals(Fortegn.T, it.avvistFortegn)

                val godkjentMedVarsel = oppdragsliste.filter {
                    it.status == OppdragStateStatus.FEIL && (getKvitteringsMelding(it)!!.mmel.alvorlighetsgrad == "04")
                }
                assertEquals(godkjentMedVarsel.map { satsSum(it) }.sum(), it.varselBelop.toLong())
                assertEquals(godkjentMedVarsel.size, it.varselAntall)
                assertEquals(Fortegn.T, it.varselFortegn)

                val mangler = oppdragsliste.filter {
                    it.status == OppdragStateStatus.SENDT_OS
                }
                assertEquals(mangler.map { satsSum(it) }.sum(), it.manglerBelop.toLong())
                assertEquals(mangler.size, it.manglerAntall)
                assertEquals(Fortegn.T, it.manglerFortegn)
            }
        }
    }

    @Test
    fun testOpprettDetaljdata() {
        val mapper = AvstemmingMapper(testoppdragsliste1, ØkonomiKodeFagområde.SYKEPENGER_REFUSJON_ARBEIDSGIVER)

        val detaljer = mapper.opprettDetaljdata()

        sjekkDetaljerForTestoppdragsliste1(detaljer)
    }

    /////////////////////
    /////////////////////
    /////////////////////

    private fun satsSum(oppdrag: OppdragStateDTO) =
        oppdrag.utbetalingsOppdrag.utbetalingsLinje.map {
            it.sats.toLong()
        }.sum()

    fun sjekkDetaljerForTestoppdragsliste1(detaljer: List<Detaljdata>) {
        assertEquals(5, detaljer.size)
        val oppdragsliste = testoppdragsliste1
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

        oppdragsliste.get(6).let { oppdrag ->
            detaljer.get(3).let { detalj ->
                assertEquals(oppdrag.id.toString(), detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.MANG, detalj.detaljType)
            }
        }

        oppdragsliste.get(7).let { oppdrag ->
            detaljer.get(4).let { detalj ->
                assertEquals(oppdrag.id.toString(), detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.MANG, detalj.detaljType)
            }
        }
    }

    internal fun getKvitteringsMelding(oppdrag: OppdragStateDTO) : Oppdrag? =
            oppdrag.oppdragResponse?.let {
                JAXBOppdrag().toOppdrag(it)
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
        val now = LocalDateTime.now().plusDays(oppdragIdSequence)
        val newId = oppdragIdSequence++
        return OppdragStateDTO(
                id = newId,
                created = now,
                modified = now,
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
                ),
                avstemming = AvstemmingDTO(
                        oppdragStateId = newId,
                        avstemt = false,
                        id = 1024,
                        nokkel = now
                )
        )
    }


}