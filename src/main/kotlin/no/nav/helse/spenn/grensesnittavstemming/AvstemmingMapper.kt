package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.FagOmraadekode
import no.nav.helse.spenn.KvitteringAlvorlighetsgrad
import no.nav.helse.spenn.avstemmingsnokkelFormatter
import no.nav.helse.spenn.oppdrag.dao.OppdragStateStatus
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

class AvstemmingMapper(
        private val oppdragsliste:List<OppdragStateDTO>,
        private val fagområde:FagOmraadekode,
        private val jaxbOppdrag : JAXBOppdrag = JAXBOppdrag()
) {

    private val oppdragSorterByAvstemmingsnøkkel = Comparator<OppdragStateDTO>{ a, b ->
        when {
            avstemmingsnøkkelFor(a) > avstemmingsnøkkelFor(b) -> 1
            avstemmingsnøkkelFor(a) < avstemmingsnøkkelFor(b) -> -1
            else -> 0
        }
    }
    private val oppdragslisteSortedByAvstemmingsnøkkel = lazy {oppdragsliste.sortedWith(oppdragSorterByAvstemmingsnøkkel)}
    private val oppdragMedLavestAvstemmingsnøkkel = lazy { oppdragslisteSortedByAvstemmingsnøkkel.value.first() }
    private val oppdragMedHøyestAvstemmingsnøkkel = lazy { oppdragslisteSortedByAvstemmingsnøkkel.value.last() }

    private val oppdragslisteNokkelFom = lazy { avstemmingsnøkkelFor(oppdragMedLavestAvstemmingsnøkkel.value) }
    private val oppdragslisteNokkelTom = lazy { avstemmingsnøkkelFor(oppdragMedHøyestAvstemmingsnøkkel.value) }
    // private val tidspunktAvstemmingTom = lazy { oppdragsliste.map { tidspunktMelding(it) }.max() } // TODO: Utgår ?
    private val avstemmingId = encodeUUIDBase64(UUID.randomUUID())

    fun avstemmingsnokkelFom() = oppdragslisteNokkelFom.value

    fun avstemmingsnokkelTom() = oppdragslisteNokkelTom.value

    private val log = LoggerFactory.getLogger(AvstemmingMapper::class.java)

    companion object {
        internal val objectFactory = ObjectFactory()
        private const val SAKSBEHANDLERS_BRUKER_ID = "SPA"

        private fun tidspunktMelding(oppdrag: OppdragStateDTO) = oppdrag.avstemming!!.nokkel.format(avstemmingsnokkelFormatter)

        private val DETALJER_PR_MELDING = 70

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
                LocalDateTime.parse(localDateTimeString, avstemmingsnokkelFormatter)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

        private fun getBelop(oppdrag: OppdragStateDTO): Long =
                oppdrag.utbetalingsOppdrag.utbetalingsLinje.map { it.sats }.reduce(BigDecimal::add).toLong()

    }

    internal fun lagAvstemmingsMeldinger() : List<Avstemmingsdata> =
            if (oppdragsliste.isEmpty())
                emptyList()
            else
                (listOf(lagStartmelding()) + lagDatameldinger() + listOf(lagSluttmelding()))

    internal fun getKvitteringsMelding(oppdrag: OppdragStateDTO) : Oppdrag? =
        oppdrag.oppdragResponse?.let {
            jaxbOppdrag.toOppdrag(it)
        }

    private fun avstemmingsDetaljtypeForOppdrag(oppdrag: OppdragStateDTO) : DetaljType? =
            when (oppdrag.status) {
                OppdragStateStatus.SENDT_OS -> DetaljType.MANG
                OppdragStateStatus.FEIL -> DetaljType.AVVI
                OppdragStateStatus.SIMULERING_OK, OppdragStateStatus.STARTET, OppdragStateStatus.SIMULERING_FEIL, OppdragStateStatus.STOPPET -> {
                    log.error("Uventet status: ${oppdrag.status} på oppdragId=${oppdrag.id}. Håndterer som om 'ferdig'")
                    null
                }
                OppdragStateStatus.FERDIG -> {
                    val kvittering = getKvitteringsMelding(oppdrag)
                    if (kvittering!!.mmel.alvorlighetsgrad == KvitteringAlvorlighetsgrad.AKSEPTERT_MEN_NOE_ER_FEIL.kode)
                        DetaljType.VARS
                    else
                        null
                }
            }

    internal fun opprettDetaljdata() : List<Detaljdata> =
        oppdragsliste.mapNotNull { oppdrag ->
            val detaljTypeForOppdrag = avstemmingsDetaljtypeForOppdrag(oppdrag)
            if (detaljTypeForOppdrag == null) {
                null
            } else {
                objectFactory.createDetaljdata().apply {
                    this.detaljType = detaljTypeForOppdrag
                    this.offnr = oppdrag.utbetalingsOppdrag.oppdragGjelder
                    this.avleverendeTransaksjonNokkel = oppdrag.utbetalingsreferanse
                    this.tidspunkt = tidspunktMelding(oppdrag)
                    if (detaljType in listOf(DetaljType.AVVI, DetaljType.VARS)) {
                        val kvittering = getKvitteringsMelding(oppdrag)
                        this.meldingKode = kvittering!!.mmel.kodeMelding
                        this.alvorlighetsgrad = kvittering.mmel.alvorlighetsgrad
                        this.tekstMelding = kvittering.mmel.beskrMelding
                    }
                }
            }
        }

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
            oppdrag.avstemming!!.nokkel.format(avstemmingsnokkelFormatter)

    private fun tilAksjonsdata(aksjonType: AksjonType): Aksjonsdata {
        val aksjonsdata = objectFactory.createAksjonsdata()
        aksjonsdata.aksjonType = aksjonType
        aksjonsdata.kildeType = KildeType.AVLEV
        aksjonsdata.avstemmingType = AvstemmingType.GRSN
        aksjonsdata.avleverendeKomponentKode = ØkonomiKodekomponent.SYKEPENGEBEHANDLING.kodekomponent
        aksjonsdata.mottakendeKomponentKode = ØkonomiKodekomponent.OPPDRAGSSYSTEMET.kodekomponent
        aksjonsdata.underkomponentKode = fagområde.kode
        aksjonsdata.nokkelFom = oppdragslisteNokkelFom.value.toString()
        aksjonsdata.nokkelTom = oppdragslisteNokkelTom.value.toString()
        //aksjonsdata.tidspunktAvstemmingTom = tidspunktAvstemmingTom.value // TODO: Utgår ? : "Feltet Tidspkt-avstemming-tom skal brukes ved konsistensavstemming" ref: APP030 Interface Spesifikasjoner Avstemming ver 004, side 12
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
        periodedata.datoAvstemtFom = tilPeriodeData(tidspunktMelding(oppdragMedLavestAvstemmingsnøkkel.value))
        periodedata.datoAvstemtTom = tilPeriodeData(tidspunktMelding(oppdragMedHøyestAvstemmingsnøkkel.value))
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
            if (oppdrag.status == OppdragStateStatus.SENDT_OS) {
                manglerBelop += belop
                manglerAntall++
            } else if (KvitteringAlvorlighetsgrad.OK.kode == alvorlighetsgrad) {
                godkjentBelop += belop
                godkjentAntall++
            } else if (KvitteringAlvorlighetsgrad.AKSEPTERT_MEN_NOE_ER_FEIL.kode == alvorlighetsgrad) {
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
