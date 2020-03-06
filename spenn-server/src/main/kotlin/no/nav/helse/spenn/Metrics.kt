package no.nav.helse.spenn

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import java.math.BigDecimal

object Metrics {
    private val vedtakCounter = Counter.build()
        .name("VEDTAK")
        .help("Anntal vedtak med status")
        .labelNames("status")
        .register()

    fun tellDuplikatVedtak() {
        vedtakCounter.labels("DUPLIKAT").inc()
    }

    private val avstemmingCounter = Counter.build()
        .name("AVSTEMMING")
        .help("Informasjon om ressultat av avstemmingskall")
        .labelNames("type")
        .register()

    fun tellAvstemming(grunnlagsdata: Grunnlagsdata) {
        avstemmingCounter.labels("godkjent").inc(grunnlagsdata.godkjentAntall.toDouble())
        avstemmingCounter.labels("avvist").inc(grunnlagsdata.avvistAntall.toDouble())
        avstemmingCounter.labels("mangler").inc(grunnlagsdata.manglerAntall.toDouble())
        avstemmingCounter.labels("varsel").inc(grunnlagsdata.varselAntall.toDouble())
    }

    private val oppdragCounter = Counter.build()
        .name("OPPDRAG")
        .help("Antall oppdrag sendt og forsøkt sendt til OS")
        .labelNames("status")
        .register()

    fun tellOppdrag(status: TransaksjonStatus) {
        oppdragCounter.labels(status.name).inc()
    }

    fun tellOppdragStoppet() {
        tellOppdrag(TransaksjonStatus.STOPPET)
    }

    fun tellOppdragSendt() {
        tellOppdrag(TransaksjonStatus.SENDT_OS)
    }

    private val maksbeløpGauge = Gauge.build()
        .name("SIMULERING_UTBETALT_MAKS_BELOP")
        .help("Det høyeste simulerte beløpet")
        .register()

    fun setMaksbeløpGauge(totalBelop: BigDecimal) {
        if (maksbeløpGauge.get() < totalBelop.toDouble()) maksbeløpGauge.set(totalBelop.toDouble())
    }

    private val simuleringCounter = Counter.build()
        .name("SIMULERING")
        .help("Antall simuleringer med simuleringsstatus")
        .labelNames("status")
        .register()

    fun countSimulering(simuleringStatus: SimuleringStatus) {
        simuleringCounter.labels(simuleringStatus.name).inc()
    }

    private val utbetaltBeløpCounter = Counter.build()
        .name("SIMULERING_UTBETALT_BELOP")
        .help("Totalt utbetalt")
        .register()

    fun countUtbetaltBeløp(totalBelop: BigDecimal) {
        utbetaltBeløpCounter.inc(totalBelop.toDouble())
    }

    private val simuleringstimer = Gauge.build()
        .name("simulering")
        .help("Tid brukt pr. simulering")
        .register()

    fun <T> timeSimulering(f: () -> T): T {
        val timer = simuleringstimer.startTimer()
        val result = f()
        timer.close()
        return result
    }
}
