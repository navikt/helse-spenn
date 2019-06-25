package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.*
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.UUID


enum class ØkonomiKodekomponent(val kodekomponent : String) {
    SYKEPENGEBEHANDLING("SP"),
    OPPDRAGSSYSTEMET("OS")
}

enum class ØkonomiKodeFagområde(val kode: String) {
    SYKEPENGER("SP"),
    SYKEPENGER_REFUSJON_ARBEIDSGIVER("SPREF")
}

class AvstemmingMapper(
        private val oppdragsliste:List<OppdragStateDTO>,
        private val fagområde:ØkonomiKodeFagområde,
        private val jaxbOppdrag : JAXBOppdrag = JAXBOppdrag(),
        private val jaxbAvstemmingsdata : JAXBAvstemmingsdata = JAXBAvstemmingsdata()
) {

    private val oppdragSorterByAvstemmingsnøkkel = Comparator<OppdragStateDTO>{ a, b ->
        when {
            avstemmingsnøkkelFor(a) > avstemmingsnøkkelFor(b) -> 1
            avstemmingsnøkkelFor(a) < avstemmingsnøkkelFor(b) -> -1
            else -> 0
        }
    }
    private val oppdragslisteSortedByAvstemmingsnøkkel = oppdragsliste.sortedWith(oppdragSorterByAvstemmingsnøkkel)
    private val oppdragMedLavestAvstemmingsnøkkel = oppdragslisteSortedByAvstemmingsnøkkel.first()
    private val oppdragMedHøyestAvstemmingsnøkkel = oppdragslisteSortedByAvstemmingsnøkkel.last()

    private val oppdragslisteNokkelFom = avstemmingsnøkkelFor(oppdragMedLavestAvstemmingsnøkkel)
    private val oppdragslisteNokkelTom = avstemmingsnøkkelFor(oppdragMedHøyestAvstemmingsnøkkel)
    private val tidspunktAvstemmingTom = oppdragsliste.map { tidspunktMelding(it) }.max()
    private val avstemmingId = encodeUUIDBase64(UUID.randomUUID())



    private val log = LoggerFactory.getLogger(AvstemmingMapper::class.java)

    companion object {
        internal val objectFactory = ObjectFactory()
        private const val SAKSBEHANDLERS_BRUKER_ID = "SPA" // TODO: Dobbelsjekk med øko at dette er greit

        private val tidspunktFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS") // TODO: Duplisert fra OppdragMapper: trekk ut ?

        private fun tidspunktMelding(oppdrag: OppdragStateDTO) = oppdrag.modified.format(tidspunktFormatter)  // TODO: created vs modified ? bruk oppdragDTO.avstemming115.tidspunktMelding (?) / Finn riktig verdi (fpsak bruker senestAvstemming115.getTidspnktMelding())

        private val DETALJER_PR_MELDING = 70 // ref: fpsak.grensesnittavtemmingsMapper

        /**
         * ref: FPSAK:ØkonomistøtteUtils
         */
        /*private fun tilSpesialkodetDatoOgKlokkeslett(dt: LocalDateTime): String =
            dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS"))
*/
        /**
         * Kopiert fra FPSAK:GrensesnittavstemmingMapper (TODO: Sjekk om dette er nødvendig)
         */
        private fun encodeUUIDBase64(uuid: UUID): String {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
        }

        private fun tilFortegn(belop: Long): Fortegn {
            return if (belop >= 0) Fortegn.T else Fortegn.F
        }

        internal fun List<OppdragStateDTO>.tilTotaldata() : Totaldata {
            val totalBeløp = this.flatMap { it.utbetalingsOppdrag.utbetalingsLinje.map { it.sats.toLong() } }.sum()
            val totaldata = objectFactory.createTotaldata()
            totaldata.totalAntall = this.size
            totaldata.totalBelop = BigDecimal.valueOf(totalBeløp)
            totaldata.fortegn = tilFortegn(totalBeløp)
            return totaldata
        }

        private fun tilPeriodeData(localDateTimeString: String): String =
                LocalDateTime.parse(localDateTimeString, tidspunktFormatter)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

        private fun getBelop(oppdrag: OppdragStateDTO): Long =
                oppdrag.utbetalingsOppdrag.utbetalingsLinje.map { it.sats }.reduce(BigDecimal::add).toLong()

    }

    internal fun lagAvstemmingsMeldinger() : List<Avstemmingsdata> {
        throw Exception("UGH!")
    }

    internal fun getKvitteringsMelding(oppdrag: OppdragStateDTO) : Oppdrag? =
        oppdrag.oppdragResponse?.let {
            jaxbOppdrag.toOppdrag(it)
        }

    internal fun opprettDetaljdata() : List<Detaljdata> =
        oppdragsliste.mapNotNull { oppdrag ->
            when (oppdrag.status) {
                OppdragStateStatus.SENDT_OS, OppdragStateStatus.FEIL ->
                    objectFactory.createDetaljdata().apply {
                        val kvittering = getKvitteringsMelding(oppdrag)
                        this.detaljType = if (oppdrag.status == OppdragStateStatus.SENDT_OS || kvittering == null) {
                            if (!(oppdrag.status == OppdragStateStatus.SENDT_OS && kvittering == null)) {
                                log.error("inkonsistente data på oppdragId=${oppdrag.id}: status=${oppdrag.status} kvitteringFinnes=${kvittering!=null}")
                            }
                            DetaljType.MANG
                        } else {
                            this.meldingKode = kvittering.mmel.kodeMelding
                            this.alvorlighetsgrad = kvittering.mmel.alvorlighetsgrad
                            this.tekstMelding = kvittering.mmel.beskrMelding
                            if (kvittering.oppdrag110?.fagsystemId != oppdrag.id.toString()) {
                                log.error("mismatch mellom fagsystemid fra kvittering.oppdrag110 (${kvittering.oppdrag110?.fagsystemId}) og fra OppdragStateDTO (${oppdrag.id})")
                            }
                            if (kvittering.mmel.alvorlighetsgrad == "04")
                                DetaljType.VARS
                            else
                                DetaljType.AVVI
                        }
                        this.offnr = oppdrag.utbetalingsOppdrag.oppdragGjelder //detaljdata.setOffnr(oppdrag110.getOppdragGjelderId());
                        this.avleverendeTransaksjonNokkel = oppdrag.id.toString()
                        this.tidspunkt = tidspunktMelding(oppdrag)
                    }
                OppdragStateStatus.FERDIG -> null
                OppdragStateStatus.SIMULERING_OK, OppdragStateStatus.STARTET, OppdragStateStatus.SIMULERING_FEIL -> {
                    log.error("Uventet status: ${oppdrag.status} på oppdragId=${oppdrag.id}. Håndterer som om 'ferdig'")
                    null
                }
            }
        }


    fun lagXmlMeldinger() : List<String> =
        (listOf(lagStartmelding()) + lagDatameldinger() + listOf(lagSluttmelding()))
                .map { jaxbAvstemmingsdata.fromAvstemmingsdataToXml(it) }

    internal fun lagStartmelding() = lagAvstemmingsdataFelles(AksjonType.START)

    internal fun lagSluttmelding() = lagAvstemmingsdataFelles(AksjonType.AVSL)

    internal fun lagDatameldinger(): List<Avstemmingsdata> {
        val detaljMeldinger = opprettDetaljdata().chunked(DETALJER_PR_MELDING).map {
            lagAvstemmingsdataFelles(AksjonType.DATA).apply {
                this.detalj.addAll(it)
            }
        }

        val avstemmingsdataListe = if (detaljMeldinger.isNotEmpty()) detaljMeldinger else listOf(lagAvstemmingsdataFelles(AksjonType.DATA))
        avstemmingsdataListe.first().apply {
            this.total = opprettTotaldata()
            this.periode = opprettPeriodedata()
            this.grunnlag = opprettGrunnlagsdata()
        }

        return avstemmingsdataListe
    }

    internal fun lagAvstemmingsdataFelles(aksjonType: AksjonType): Avstemmingsdata =
            objectFactory.createAvstemmingsdata().apply {
                aksjon = tilAksjonsdata(aksjonType)
            }


    private fun avstemmingsnøkkelFor(oppdrag: OppdragStateDTO) =
            oppdrag.avstemming?.nokkel?:throw Exception("oppdrag uten avstemmingsnøkkel: ${oppdrag.id}")


    //.oppdrag110.avstemming115.tidspktMelding

    private fun tilAksjonsdata(aksjonType: AksjonType): Aksjonsdata {
        val aksjonsdata = objectFactory.createAksjonsdata()
        aksjonsdata.aksjonType = aksjonType
        aksjonsdata.kildeType = KildeType.AVLEV
        aksjonsdata.avstemmingType = AvstemmingType.GRSN
        aksjonsdata.avleverendeKomponentKode = ØkonomiKodekomponent.SYKEPENGEBEHANDLING.kodekomponent
        aksjonsdata.mottakendeKomponentKode = ØkonomiKodekomponent.OPPDRAGSSYSTEMET.kodekomponent
        aksjonsdata.underkomponentKode = fagområde.kode
        aksjonsdata.nokkelFom = oppdragslisteNokkelFom.toString()
        aksjonsdata.nokkelTom = oppdragslisteNokkelTom.toString()
        aksjonsdata.tidspunktAvstemmingTom = tidspunktAvstemmingTom //?.let {tilSpesialkodetDatoOgKlokkeslett(it)}
        aksjonsdata.avleverendeAvstemmingId = avstemmingId
        aksjonsdata.brukerId = SAKSBEHANDLERS_BRUKER_ID

        return aksjonsdata
    }

    private fun opprettTotaldata(): Totaldata {
        val totalBelop = oppdragsliste.map { getBelop(it) }.sum()
        val totaldata = objectFactory.createTotaldata()
        totaldata.totalAntall = oppdragsliste.size
        totaldata.totalBelop = BigDecimal.valueOf(totalBelop)
        totaldata.fortegn = tilFortegn(totalBelop)
        return totaldata
    }

    private fun opprettPeriodedata(): Periodedata {
        val periodedata = objectFactory.createPeriodedata()
        periodedata.datoAvstemtFom = tilPeriodeData(tidspunktMelding(oppdragMedLavestAvstemmingsnøkkel))
        periodedata.datoAvstemtTom = tilPeriodeData(tidspunktMelding(oppdragMedHøyestAvstemmingsnøkkel))
        return periodedata
    }

    private fun opprettGrunnlagsdata(): Grunnlagsdata {
        var godkjentAntall = 0
        var godkjentBelop = 0L
        var varselAntall = 0
        var varselBelop = 0L
        var avvistAntall = 0
        var avvistBelop = 0L
        var manglerAntall = 0
        var manglerBelop = 0L
        for (oppdrag in oppdragsliste) {
            val belop = getBelop(oppdrag)
            val kvittering = getKvitteringsMelding(oppdrag)
            val alvorlighetsgrad = kvittering?.mmel?.alvorlighetsgrad
            if (oppdrag.status == OppdragStateStatus.SENDT_OS) /*or kvittering is null?*/ {
                manglerBelop += belop
                manglerAntall++
            } else if ("00" == alvorlighetsgrad) {
                godkjentBelop += belop
                godkjentAntall++
            } else if ("04" == alvorlighetsgrad) {
                varselBelop += belop
                varselAntall++
            } else {
                avvistBelop += belop
                avvistAntall++
            }
        }
        val grunnlagsdata = objectFactory.createGrunnlagsdata()

        grunnlagsdata.godkjentAntall = godkjentAntall
        grunnlagsdata.godkjentBelop = BigDecimal.valueOf(godkjentBelop)
        grunnlagsdata.godkjentFortegn = tilFortegn(godkjentBelop)

        grunnlagsdata.varselAntall = varselAntall
        grunnlagsdata.varselBelop = BigDecimal.valueOf(varselBelop)
        grunnlagsdata.varselFortegn = tilFortegn(varselBelop)

        grunnlagsdata.avvistAntall = avvistAntall
        grunnlagsdata.avvistBelop = BigDecimal.valueOf(avvistBelop)
        grunnlagsdata.avvistFortegn = tilFortegn(avvistBelop)

        grunnlagsdata.manglerAntall = manglerAntall
        grunnlagsdata.manglerBelop = BigDecimal.valueOf(manglerBelop)
        grunnlagsdata.manglerFortegn = tilFortegn(manglerBelop)

        return grunnlagsdata
    }

}
