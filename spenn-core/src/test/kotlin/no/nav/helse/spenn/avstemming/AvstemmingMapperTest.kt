package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.core.FagOmraadekode
import no.nav.helse.spenn.core.avstemmingsnokkelFormatter
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.TransaksjonStatus.*
import no.nav.helse.spenn.oppdrag.dao.TransaksjonDTO
import no.nav.helse.spenn.testsupport.etEnkeltBehov
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
import kotlin.test.assertEquals

class AvstemmingMapperTest {

    private val jaxbAvstemming = JAXBAvstemmingsdata()

    private var oppdragIdSequence = 1L
    private var utbetalingsLinjeIdSequence = 1L

    private val testoppdragsliste1 = listOf(
        lagOppdrag(status = FERDIG, dagSats = 1000, utbetalingsreferanse = "1"),
        lagOppdrag(status = FEIL, alvorlighetsgrad = "08", dagSats = 1000, utbetalingsreferanse = "2"),
        lagOppdrag(status = FERDIG, alvorlighetsgrad = "04", dagSats = 1100, utbetalingsreferanse = "3"),
        lagOppdrag(status = FEIL, alvorlighetsgrad = "12", dagSats = 1200, utbetalingsreferanse = "4"),
        lagOppdrag(status = FERDIG, dagSats = 1300, utbetalingsreferanse = "5"),
        lagAnnuleringsOppdrag(status = FERDIG, utbetalingsreferanse = "AAA"),
        lagAnnuleringsOppdrag(status = FEIL, alvorlighetsgrad = "08", utbetalingsreferanse = "BBB"),
        lagAnnuleringsOppdrag(status = SENDT_OS, utbetalingsreferanse = "CCC"),
        lagOppdrag(status = FERDIG, dagSats = 1400, utbetalingsreferanse = "6"),
        lagOppdrag(status = SENDT_OS, dagSats = 1550, utbetalingsreferanse = "7"),
        lagOppdrag(status = SENDT_OS, dagSats = 1660, utbetalingsreferanse = "8")
    )

    @Test
    fun avstemmingMapperBørTakleTomListe() {
        val mapper = AvstemmingMapper(
            emptyList(),
            FagOmraadekode.SYKEPENGER_REFUSJON
        )
        val meldinger = mapper.lagAvstemmingsMeldinger()
        assertEquals(0, meldinger.size)
    }

    @Test
    fun testAvstemmingAvKunEtAnnulertOppdrag() {
        val oppdragsliste = listOf(lagAnnuleringsOppdrag(utbetalingsreferanse = "AAA"))
        val mapper = AvstemmingMapper(
            oppdragsliste,
            FagOmraadekode.SYKEPENGER_REFUSJON
        )
        val xmlMeldinger = mapper.lagAvstemmingsMeldinger().map { jaxbAvstemming.fromAvstemmingsdataToXml(it) }
        println(xmlMeldinger)
        val meldinger = xmlMeldinger.map { JAXBAvstemmingsdata().toAvstemmingsdata(it) }
        assertEquals(3, meldinger.size)
        meldinger[1].total.let {
            assertEquals(BigDecimal.ZERO, it.totalBelop)
            assertEquals(1, it.totalAntall)
        }
    }

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
            assertEquals(oppdragsliste.map { it.nokkel!!.format(avstemmingsnokkelFormatter) }.min(), aksjon.nokkelFom)
            assertEquals(oppdragsliste.map { it.nokkel!!.format(avstemmingsnokkelFormatter) }.max(), aksjon.nokkelTom)
            //assertEquals(oppdragsliste.map { it.modified.format(tidspunktFormatter) }.max(), aksjon.tidspunktAvstemmingTom) // TODO: Utgår?
            assertEquals("SPA", aksjon.brukerId)
        }

        val mapper = AvstemmingMapper(
            oppdragsliste,
            FagOmraadekode.SYKEPENGER_REFUSJON
        )
        val xmlMeldinger = mapper.lagAvstemmingsMeldinger().map { jaxbAvstemming.fromAvstemmingsdataToXml(it) }
        //println(xmlMeldinger)
        val meldinger = xmlMeldinger.map { JAXBAvstemmingsdata().toAvstemmingsdata(it) }
        assertEquals(3, meldinger.size)
        sjekkAksjon(meldinger.first().aksjon, AksjonType.START)
        sjekkAksjon(meldinger.last().aksjon, AksjonType.AVSL)

        meldinger[1].let { avstemmingsdata ->
            sjekkAksjon(avstemmingsdata.aksjon, AksjonType.DATA)
            sjekkDetaljerForTestoppdragsliste1(avstemmingsdata.detalj)
            avstemmingsdata.total.let { totaldata ->
                assertEquals(Fortegn.T, totaldata.fortegn)
                assertEquals(oppdragsliste.size, totaldata.totalAntall)
                assertEquals(oppdragsliste.map { transaksjonDTO ->
                    transaksjonDTO.utbetalingsOppdrag.utbetaling?.utbetalingsLinjer?.map {
                        it.sats.toLong()
                    }?.sum() ?: 0
                }.sum(), totaldata.totalBelop.toLong())
            }
            avstemmingsdata.periode.let { periodedata ->
                assertEquals(
                    oppdragsliste.map { it.nokkel!! }.min()!!.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
                    periodedata.datoAvstemtFom
                )
                assertEquals(
                    oppdragsliste.map { it.nokkel!! }.max()!!.format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
                    periodedata.datoAvstemtTom
                )
            }
            avstemmingsdata.grunnlag.let { grunnlagsdata ->
                val ferdige =
                    oppdragsliste.filter { it.status == FERDIG && (getKvitteringsMelding(it)!!.mmel.alvorlighetsgrad == "00") }
                assertEquals(ferdige.map { satsSum(it) }.sum(), grunnlagsdata.godkjentBelop.toLong())
                assertEquals(ferdige.size, grunnlagsdata.godkjentAntall)
                assertEquals(Fortegn.T, grunnlagsdata.godkjentFortegn)

                val avviste = oppdragsliste.filter {
                    it.status == FEIL && (getKvitteringsMelding(it)!!.mmel.alvorlighetsgrad in listOf("08", "12"))
                }
                assertEquals(avviste.map { satsSum(it) }.sum(), grunnlagsdata.avvistBelop.toLong())
                assertEquals(avviste.size, grunnlagsdata.avvistAntall)
                assertEquals(Fortegn.T, grunnlagsdata.avvistFortegn)

                val godkjentMedVarsel = oppdragsliste.filter {
                    it.status == FERDIG && (getKvitteringsMelding(it)!!.mmel.alvorlighetsgrad == "04")
                }
                assertEquals(godkjentMedVarsel.map { satsSum(it) }.sum(), grunnlagsdata.varselBelop.toLong())
                assertEquals(godkjentMedVarsel.size, grunnlagsdata.varselAntall)
                assertEquals(Fortegn.T, grunnlagsdata.varselFortegn)

                val mangler = oppdragsliste.filter {
                    it.status == SENDT_OS
                }
                assertEquals(mangler.map { satsSum(it) }.sum(), grunnlagsdata.manglerBelop.toLong())
                assertEquals(mangler.size, grunnlagsdata.manglerAntall)
                assertEquals(Fortegn.T, grunnlagsdata.manglerFortegn)
            }
        }
    }

    @Test
    fun testOpprettDetaljdata() {
        val mapper = AvstemmingMapper(
            testoppdragsliste1,
            FagOmraadekode.SYKEPENGER_REFUSJON
        )

        val detaljer = mapper.opprettDetaljdata()

        sjekkDetaljerForTestoppdragsliste1(detaljer)
    }

    /////////////////////
    /////////////////////
    /////////////////////

    companion object {
        internal fun lagOppdragResponseXml(
            fagsystemId: String,
            manglerRespons: Boolean = false,
            alvorlighetsgrad: String
        ): String? {
            if (manglerRespons) {
                return null
            }
            val kvittering = Oppdrag()
            kvittering.mmel = Mmel()
            kvittering.mmel.kodeMelding = "Melding"
            kvittering.mmel.alvorlighetsgrad = alvorlighetsgrad
            kvittering.mmel.beskrMelding = "Beskrivelse"
            kvittering.oppdrag110 = Oppdrag110()
            kvittering.oppdrag110.fagsystemId = fagsystemId
            return OppdragXml.marshal(kvittering)
        }
    }

    private fun satsSum(oppdrag: TransaksjonDTO) =
        oppdrag.utbetalingsOppdrag.utbetaling?.utbetalingsLinjer?.map {
            it.sats.toLong()
        }?.sum() ?: 0

    fun sjekkDetaljerForTestoppdragsliste1(detaljer: List<Detaljdata>) {
        assertEquals(7, detaljer.size, "Forventet 7 avvik")
        val oppdragsliste = testoppdragsliste1
        oppdragsliste.get(1).let { oppdrag ->
            detaljer.get(0).let { detalj ->
                assertEquals("08", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.AVVI, detalj.detaljType)
            }
        }
        oppdragsliste.get(2).let { oppdrag ->
            detaljer.get(1).let { detalj ->
                assertEquals("04", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.VARS, detalj.detaljType)
            }
        }

        oppdragsliste.get(3).let { oppdrag ->
            detaljer.get(2).let { detalj ->
                assertEquals("12", detalj.alvorlighetsgrad)
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.AVVI, detalj.detaljType)
            }
        }
/*
            lagOppdrag(status = FERDIG, dagSats = 1000, utbetalingsreferanse = "1"),
            lagOppdrag(status = FEIL, alvorlighetsgrad = "08", dagSats = 1000, utbetalingsreferanse = "2"),
            lagOppdrag(status = FERDIG, alvorlighetsgrad = "04", dagSats = 1100, utbetalingsreferanse = "3"),
            lagOppdrag(status = FEIL, alvorlighetsgrad = "12", dagSats = 1200, utbetalingsreferanse = "4"),
            lagOppdrag(status = FERDIG, dagSats = 1300, utbetalingsreferanse = "5"),
            lagAnnuleringsOppdrag(status = FERDIG, utbetalingsreferanse = "AAA"),
            lagAnnuleringsOppdrag(status = FEIL, utbetalingsreferanse = "BBB"),
            lagAnnuleringsOppdrag(status = SENDT_OS, utbetalingsreferanse = "CCC"),
            lagOppdrag(status = FERDIG, dagSats = 1400, utbetalingsreferanse = "6"),
            lagOppdrag(status = SENDT_OS, dagSats = 1550, utbetalingsreferanse = "7"),
            lagOppdrag(status = SENDT_OS, dagSats = 1660, utbetalingsreferanse = "8")
 */

        oppdragsliste.get(6).let { oppdrag ->
            detaljer.get(3).let { detalj ->
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.AVVI, detalj.detaljType)
            }
        }
        oppdragsliste.get(7).let { oppdrag ->
            detaljer.get(4).let { detalj ->
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.MANG, detalj.detaljType)
            }
        }

        oppdragsliste.get(9).let { oppdrag ->
            detaljer.get(5).let { detalj ->
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.MANG, detalj.detaljType)
            }
        }

        oppdragsliste.get(10).let { oppdrag ->
            detaljer.get(6).let { detalj ->
                assertEquals(oppdrag.utbetalingsreferanse, detalj.avleverendeTransaksjonNokkel)
                assertEquals(DetaljType.MANG, detalj.detaljType)
            }
        }
    }

    internal fun getKvitteringsMelding(oppdrag: TransaksjonDTO): Oppdrag? =
        oppdrag.oppdragResponse?.let {
            OppdragXml.unmarshal(it)
        }


    private fun lagAnnuleringsOppdrag(
        status: TransaksjonStatus = FERDIG,
        utbetalingsreferanse: String,
        alvorlighetsgrad: String = "00"
    ): TransaksjonDTO {
        val now = LocalDateTime.now().plusDays(oppdragIdSequence)
        val newId = oppdragIdSequence++
        return TransaksjonDTO(
            id = newId,
            oppdragResponse = lagOppdragResponseXml(
                utbetalingsreferanse,
                status == SENDT_OS,
                alvorlighetsgrad
            ),
            simuleringresult = null,
            utbetalingsreferanse = utbetalingsreferanse,
            status = status,
            utbetalingsOppdrag = UtbetalingsOppdrag(
                oppdragGjelder = "12121210010",
                utbetaling = null,
                utbetalingsreferanse = "1001",
                saksbehandler = "yes",
                behov = etEnkeltBehov().toString()
            ),
            avstemt = false,
            nokkel = now,
            created = LocalDateTime.now()
        )
    }

    private fun lagOppdrag(
        status: TransaksjonStatus = FERDIG,
        utbetalingsreferanse: String, // = this.utbetalingsreferanse,
        alvorlighetsgrad: String = "00",
        dagSats: Long = 1345
    ): TransaksjonDTO {
        val now = LocalDateTime.now().plusDays(oppdragIdSequence)
        val newId = oppdragIdSequence++
        return TransaksjonDTO(
            id = newId,
            oppdragResponse = lagOppdragResponseXml(
                utbetalingsreferanse,
                status == SENDT_OS,
                alvorlighetsgrad
            ),
            simuleringresult = null,
            utbetalingsreferanse = utbetalingsreferanse,
            status = status,
            utbetalingsOppdrag = UtbetalingsOppdrag(
                oppdragGjelder = "12121210010",
                utbetaling = Utbetaling(
                    organisasjonsnummer = "897654321",
                    maksdato = LocalDate.now().plusYears(1),
                    utbetalingsLinjer = listOf(
                        UtbetalingsLinje(
                            id = (utbetalingsLinjeIdSequence++).toString(),
                            datoFom = LocalDate.now().minusWeeks(4),
                            datoTom = LocalDate.now().minusWeeks(1),
                            grad = BigInteger.valueOf(100),
                            sats = BigDecimal.valueOf(dagSats),
                            satsTypeKode = SatsTypeKode.DAGLIG,
                            utbetalesTil = "999988887"
                        )
                    )
                ),
                utbetalingsreferanse = "1001",
                saksbehandler = "yes",
                behov = etEnkeltBehov().toString()
            ),
            avstemt = false,
            nokkel = now,
                    created = LocalDateTime.now()
        )
    }


}
