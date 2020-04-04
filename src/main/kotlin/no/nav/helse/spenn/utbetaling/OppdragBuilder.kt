package no.nav.helse.spenn.utbetaling

import no.nav.helse.spenn.*
import no.trygdeetaten.skjema.oppdrag.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeFactory

internal class OppdragBuilder(private val saksbehandler: String,
                              private val maksdato: LocalDate,
                              private val avstemmingsnøkkel: Long,
                              utbetalingslinjer: Utbetalingslinjer,
                              tidspunkt: Instant = Instant.now()) :
    UtbetalingslinjerVisitor {

    private companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
            .withZone(ZoneId.systemDefault())
        private val datatypeFactory = DatatypeFactory.newInstance()

        private fun LocalDate.asXmlGregorianCalendar() =
            datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(this.atStartOfDay(ZoneId.systemDefault())))
    }

    private val oppdrag = Oppdrag().apply {
        oppdrag110 = Oppdrag110().apply {
            kodeAksjon = AksjonsKode.OPPDATER.kode
            kodeFagomraade = "SPREF"
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            datoOppdragGjelderFom = LocalDate.EPOCH.asXmlGregorianCalendar()
            saksbehId = saksbehandler
            avstemming115 = Avstemming115().apply {
                nokkelAvstemming = "$avstemmingsnøkkel"
                tidspktMelding = tidsstempel.format(tidspunkt)
                kodeKomponent = KomponentKode.SYKEPENGER.kode
            }
            oppdragsEnhet120.add(OppdragsEnhet120().apply {
                enhet = OppdragSkjemaConstants.SP_ENHET
                typeEnhet = OppdragSkjemaConstants.BOS
                datoEnhetFom = LocalDate.EPOCH.asXmlGregorianCalendar()
            })
        }
    }

    init {
        utbetalingslinjer.accept(this)
    }

    fun build() = oppdrag

    override fun preVisitUtbetalingslinjer(
        utbetalingslinjer: Utbetalingslinjer,
        utbetalingsreferanse: String,
        fødselsnummer: String,
        forlengelse: Boolean
    ) {
        oppdrag.oppdrag110.kodeEndring = if (forlengelse) EndringsKode.UENDRET.kode else EndringsKode.NY.kode
        oppdrag.oppdrag110.fagsystemId = utbetalingsreferanse
        oppdrag.oppdrag110.oppdragGjelderId = fødselsnummer
    }

    override fun visitRefusjonTilArbeidsgiver(
        refusjonTilArbeidsgiver: Utbetalingslinjer.Utbetalingslinje.RefusjonTilArbeidsgiver,
        id: Int,
        organisasjonsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {
        oppdrag.oppdrag110.oppdragsLinje150.add(somOppdragslinje(id, forlengelse, fom, tom, dagsats, grad).apply {
            refusjonsinfo156 = Refusjonsinfo156().apply {
                refunderesId = organisasjonsnummer.padStart(11, '0')
                datoFom = datoVedtakFom
                maksDato = maksdato.asXmlGregorianCalendar()
            }
        })
    }

    override fun visitUtbetalingTilBruker(
        utbetalingTilBruker: Utbetalingslinjer.Utbetalingslinje.UtbetalingTilBruker,
        id: Int,
        fødselsnummer: String,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) {
        oppdrag.oppdrag110.oppdragsLinje150.add(somOppdragslinje(id, forlengelse, fom, tom, dagsats, grad).apply {
            utbetalesTilId = fødselsnummer
        })
    }

    private fun somOppdragslinje(
        id: Int,
        forlengelse: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Int
    ) = OppdragsLinje150().apply {
        delytelseId = "$id"
        kodeEndringLinje = if (forlengelse) EndringsKode.ENDRING.kode else EndringsKode.NY.kode
        kodeKlassifik = KlassifiseringsKode.SPREFAG_IOP.kode
        datoVedtakFom = fom.asXmlGregorianCalendar()
        datoVedtakTom = tom.asXmlGregorianCalendar()
        sats = dagsats.toBigDecimal()
        fradragTillegg = TfradragTillegg.T
        typeSats = SatsTypeKode.DAGLIG.kode
        saksbehId = saksbehandler
        brukKjoreplan = "N"
        grad170.add(Grad170().apply {
            typeGrad = GradTypeKode.UFØREGRAD.kode
            this.grad = grad.toBigInteger()
        })
        attestant180.add(Attestant180().apply {
            attestantId = saksbehandler
        })
    }
}
