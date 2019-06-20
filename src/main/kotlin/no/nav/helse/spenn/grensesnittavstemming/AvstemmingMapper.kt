package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.vedtak.OppdragStateDTO
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
        private val jaxbOppdrag : JAXBOppdrag = JAXBOppdrag()
) {

    private val oppdragslisteNokkelFom = oppdragsliste.map { avstemmingsnøkkelFor(it)}.min()
    private val oppdragslisteNokkelTom = oppdragsliste.map { avstemmingsnøkkelFor(it)}.max()
    private val tidspunktAvstemmingTom = oppdragsliste.map { tidspunktMelding(it) }.max()
    private val avstemmingId = encodeUUIDBase64(UUID.randomUUID())

    private val log = LoggerFactory.getLogger(AvstemmingMapper::class.java)

    companion object {
        internal val objectFactory = ObjectFactory()
        private const val SAKSBEHANDLERS_BRUKER_ID = "SPA" // TODO: Dobbelsjekk med øko at dette er greit

        private fun tidspunktMelding(oppdrag: OppdragStateDTO) = oppdrag.created  // TODO: Finn riktig verdi (fpsak bruker senestAvstemming115.getTidspnktMelding())

        /**
         * ref: FPSAK:ØkonomistøtteUtils
         */
        private fun tilSpesialkodetDatoOgKlokkeslett(dt: LocalDateTime): String =
            dt.let {
                it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS"))
            }

        /**
         * Kopiert fra FPSAK:GrensesnittavstemmingMapper (TODO: Sjekk om dette er nødvendig)
         */
        private fun encodeUUIDBase64(uuid: UUID): String {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
        }

        internal fun List<OppdragStateDTO>.tilTotaldata() : Totaldata {
            val totalBeløp = this.flatMap { it.utbetalingsOppdrag.utbetalingsLinje.map { it.sats.toLong() } }.sum()
            val totaldata = objectFactory.createTotaldata()
            totaldata.totalAntall = this.size
            totaldata.totalBelop = BigDecimal.valueOf(totalBeløp)
            totaldata.fortegn = if (totalBeløp >= 0) Fortegn.T else Fortegn.F
            return totaldata
        }

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
                        this.tidspunkt = tilSpesialkodetDatoOgKlokkeslett(tidspunktMelding(oppdrag))
                    }
                OppdragStateStatus.FERDIG -> null
                OppdragStateStatus.SIMULERING_OK, OppdragStateStatus.STARTET -> {
                    log.error("Uventet status: ${oppdrag.status} på oppdragId=${oppdrag.id}. Håndterer som om 'ferdig'")
                    null
                }
            }
        }


    internal fun lagDetaljdataFraKvittering(kvitteringsRespons: Oppdrag) {
        kvitteringsRespons.mmel.alvorlighetsgrad
        kvitteringsRespons.mmel.kodeMelding
        kvitteringsRespons.mmel.beskrMelding
        kvitteringsRespons.oppdrag110.fagsystemId // = OppdragState.id
        val behandlingId = kvitteringsRespons.oppdrag110.oppdragsLinje150.first().henvisning // FPSAK.fraOppdragLinje150 bruker Long.valueOf

    }
/*
    private fun opprettDetalj(avstemmingsdata: Avstemmingsdata, oppdrag110: Oppdrag110, detaljType: DetaljType, alvorlighetsgrad: String) {
        val kvittering = oppdrag110.getOppdragKvittering()
        val meldingKode = if (kvittering != null) kvittering!!.getMeldingKode() else null
        val beskrMelding = if (kvittering != null) kvittering!!.getBeskrMelding() else null
        val detaljdata = objectFactory.createDetaljdata()
        detaljdata.detaljType = detaljType
        detaljdata.offnr = oppdrag110.oppdragGjelderId
        detaljdata.avleverendeTransaksjonNokkel = oppdrag110.fagsystemId.toString()
        detaljdata.meldingKode = meldingKode
        detaljdata.alvorlighetsgrad = alvorlighetsgrad
        detaljdata.tekstMelding = beskrMelding
        detaljdata.tidspunkt = oppdrag110.avstemming115.getTidspnktMelding()
        avstemmingsdata.detalj.add(detaljdata)


        oppdragsliste.first().utbetalingsOppdrag.oppdragGjelder

    }*/

    private fun lagDatameldinger() {

    }

    private fun lagStartmelding() = lagAvstemmingsdataFelles(AksjonType.START)


    private fun lagSluttmelding() = lagAvstemmingsdataFelles(AksjonType.AVSL)


    internal fun lagAvstemmingsdataFelles(aksjonType: AksjonType): Avstemmingsdata =
            objectFactory.createAvstemmingsdata().apply {
                aksjon = tilAksjonsdata(aksjonType)
            }


    @Deprecated("returnerer bare oppdrag.id inntill vi har dedikert avstemmingsnøkkel på plass")
    private inline fun avstemmingsnøkkelFor(oppdrag: OppdragStateDTO) : Long =
        oppdrag.id?:throw Exception("oppdrag uten id: $oppdrag")


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
        aksjonsdata.tidspunktAvstemmingTom = tidspunktAvstemmingTom?.let {tilSpesialkodetDatoOgKlokkeslett(it)}
        aksjonsdata.avleverendeAvstemmingId = avstemmingId
        aksjonsdata.brukerId = SAKSBEHANDLERS_BRUKER_ID

        return aksjonsdata
    }

}
