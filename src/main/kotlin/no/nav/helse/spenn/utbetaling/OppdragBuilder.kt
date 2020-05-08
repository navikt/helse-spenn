package no.nav.helse.spenn.utbetaling

import no.nav.helse.spenn.Utbetalingslinjer
import no.trygdeetaten.skjema.oppdrag.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeFactory

internal class OppdragBuilder(private val utbetalingslinjer: Utbetalingslinjer,
                              private val avstemmingsnøkkel: Long,
                              tidspunkt: Instant = Instant.now()) {
    private companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
            .withZone(ZoneId.systemDefault())
        private val datatypeFactory = DatatypeFactory.newInstance()

        private fun LocalDate.asXmlGregorianCalendar() =
            datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(this.atStartOfDay(ZoneId.systemDefault())))
    }

    private val linjeStrategy: (Utbetalingslinjer.Utbetalingslinje) -> OppdragsLinje150 = when (utbetalingslinjer) {
        is Utbetalingslinjer.RefusjonTilArbeidsgiver -> ::refusjonTilArbeidsgiver
        is Utbetalingslinjer.UtbetalingTilBruker -> ::utbetalingTilBruker
    }

    private val oppdrag110 = Oppdrag110().apply {
        kodeFagomraade = utbetalingslinjer.fagområde
        kodeEndring = utbetalingslinjer.endringskode
        fagsystemId = utbetalingslinjer.fagsystemId
        oppdragGjelderId = utbetalingslinjer.fødselsnummer
        saksbehId = utbetalingslinjer.saksbehandler
        kodeAksjon = "1"
        utbetFrekvens = "MND"
        datoOppdragGjelderFom = LocalDate.EPOCH.asXmlGregorianCalendar()
        avstemming115 = Avstemming115().apply {
            nokkelAvstemming = "$avstemmingsnøkkel"
            tidspktMelding = tidsstempel.format(tidspunkt)
            kodeKomponent = "SP"
        }
        oppdragsEnhet120.add(OppdragsEnhet120().apply {
            enhet = "8020"
            typeEnhet = "BOS"
            datoEnhetFom = LocalDate.EPOCH.asXmlGregorianCalendar()
        })
    }

    fun build(): Oppdrag {
        utbetalingslinjer.forEach { oppdrag110.oppdragsLinje150.add(linjeStrategy(it)) }
        return Oppdrag().apply {
            this.oppdrag110 = this@OppdragBuilder.oppdrag110
        }
    }

    private fun refusjonTilArbeidsgiver(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) = nyLinje(utbetalingslinje).apply {
        refusjonsinfo156 = Refusjonsinfo156().apply {
            refunderesId = utbetalingslinjer.mottaker.padStart(11, '0')
            datoFom = datoVedtakFom
            maksDato = utbetalingslinjer.maksdato?.asXmlGregorianCalendar()
        }
    }

    private fun utbetalingTilBruker(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) = nyLinje(utbetalingslinje).apply {
        utbetalesTilId = utbetalingslinjer.mottaker
    }

    private fun nyLinje(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) = OppdragsLinje150().apply {
        delytelseId = "${utbetalingslinje.delytelseId}"
        refDelytelseId = utbetalingslinje.refDelytelseId?.let { "$it" }
        refFagsystemId = utbetalingslinje.refFagsystemId?.let { it }
        kodeEndringLinje = utbetalingslinje.endringskode
        kodeKlassifik = utbetalingslinje.klassekode
        datoVedtakFom = utbetalingslinje.fom.asXmlGregorianCalendar()
        datoVedtakTom = utbetalingslinje.tom.asXmlGregorianCalendar()
        kodeStatusLinje = utbetalingslinje.statuskode?.let { TkodeStatusLinje.valueOf(it) }
        datoStatusFom = utbetalingslinje.datoStatusFom?.asXmlGregorianCalendar()
        sats = utbetalingslinje.dagsats.toBigDecimal()
        fradragTillegg = TfradragTillegg.T
        typeSats = "DAG"
        saksbehId = utbetalingslinjer.saksbehandler
        brukKjoreplan = "N"
        grad170.add(Grad170().apply {
            typeGrad = "UFOR"
            grad = utbetalingslinje.grad.toBigInteger()
        })
        attestant180.add(Attestant180().apply {
            attestantId = utbetalingslinjer.saksbehandler
        })
    }
}
