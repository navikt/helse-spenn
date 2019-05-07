package no.nav.helse.spenn.oppdrag

import no.nav.helse.AppConfig

import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.defaultObjectMapper
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month

@SpringBootTest(classes = arrayOf(AppConfig::class, CxfAutoConfiguration::class,
        SimuleringConfig::class, SimuleringService::class))
class SimulerOppdragIT {

    private val log = LoggerFactory.getLogger(SimulerOppdragIT::class.java)
    @Autowired
    lateinit var simuleringService : SimuleringService


    @Test
    fun simuleringOppdragEnOppdragslinje() {
        val fom = LocalDate.of(2019, Month.APRIL, 2)
        val tom = LocalDate.of(2019, Month.APRIL, 16)
        val linje = UtbetalingsLinje(id = "1234567890", datoFom = fom,
                datoTom = tom, sats = BigDecimal.valueOf(1230), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))
        val utbetalingsOppdrag = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "995816598", utbetalingsLinje = listOf(linje))
        val simulerOppdrag = simuleringService.simulerOppdrag(utbetalingsOppdrag.toSimuleringRequest("20190408084501"))
        log.info(defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simulerOppdrag))
    }

    @Test
    fun simuleringOppdragFlereOppdragslinje() {
        val fom1 = LocalDate.of(2019, Month.JANUARY, 1)
        val tom1 = LocalDate.of(2019, Month.JANUARY, 12)
        val oppdragslinje1 = UtbetalingsLinje(id = "1", datoFom = fom1,
                datoTom =tom1, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(50))

        val fom2 = LocalDate.of(2019,Month.FEBRUARY, 13)
        val tom2 = LocalDate.of(2019,Month.FEBRUARY, 20)
        val oppdragslinje2 = UtbetalingsLinje(id = "2", datoFom = fom2,
                datoTom = tom2, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(70))

        val fom3 = LocalDate.of(2019,Month.MARCH, 18)
        val tom3 = LocalDate.of(2019, Month.APRIL, 12)
        val oppdragslinje3 = UtbetalingsLinje(id = "3", datoFom = fom3,
                datoTom = tom3, sats = BigDecimal.valueOf(1000), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))

        val utbetalingsOppdrag = UtbetalingsOppdrag(operasjon = AksjonsKode.SIMULERING,
                oppdragGjelder = "21038014495", utbetalingsLinje = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3))
        log.info(defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simuleringService.simulerOppdrag(
                utbetalingsOppdrag.toSimuleringRequest("20190408084501"))))

    }


}