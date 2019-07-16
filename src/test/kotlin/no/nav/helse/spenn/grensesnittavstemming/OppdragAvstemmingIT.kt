package no.nav.helse.spenn.grensesnittavstemming


import no.nav.helse.spenn.FagOmraadekode
import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.oppdrag.*
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Repository

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month
import java.util.*

@Repository
class MyRepo(val jooq: DSLContext) {
    fun setOppdragSequence(n:Long) {
        jooq.alterSequence("oppdragstate_id_seq").restartWith(BigInteger.valueOf(n)).execute();
    }
}

@SpringBootTest
class OppdragAvstemmingIT {

    @Autowired lateinit var mqSender: OppdragMQSender
    @Autowired lateinit var mqAvstemmingSender: AvstemmingMQSender
    @Autowired lateinit var service: OppdragStateService
    @Autowired lateinit var db: MyRepo

    @Test
    fun sendOppdragTilOS() {
        db.setOppdragSequence(101006L) // Oppdater manuelt for hver kjøring utifra faktisk siste som ble sendt til OS

        val fom1 = LocalDate.of(2019, Month.JANUARY, 1)
        val tom1 = LocalDate.of(2019, Month.JANUARY, 12)
        val oppdragslinje1 = UtbetalingsLinje(id = "1", datoFom = fom1,
                datoTom = tom1, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(50))

        val fom2 = LocalDate.of(2019, Month.FEBRUARY, 13)
        val tom2 = LocalDate.of(2019, Month.FEBRUARY, 20)
        val oppdragslinje2 = UtbetalingsLinje(id = "2", datoFom = fom2,
                datoTom = tom2, sats = BigDecimal.valueOf(600), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(70))

        val fom3 = LocalDate.of(2019, Month.MARCH, 18)
        val tom3 = LocalDate.of(2019, Month.APRIL, 12)
        val oppdragslinje3 = UtbetalingsLinje(id = "3", datoFom = fom3,
                datoTom = tom3, sats = BigDecimal.valueOf(1000), satsTypeKode = SatsTypeKode.DAGLIG,
                utbetalesTil = "995816598", grad = BigInteger.valueOf(100))

        val utbetaling = UtbetalingsOppdrag(operasjon = AksjonsKode.OPPDATER,
                oppdragGjelder = "21038014495", utbetalingsLinje = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3))

        val oppdragStateNew = OppdragStateDTO(id = null, soknadId = UUID.randomUUID(),
                utbetalingsOppdrag = utbetaling)
        val oppdragStateWithoutAvstemmingId = service.saveOppdragState(oppdragStateNew)
        val oppdragState = service.saveOppdragState(oppdragStateWithoutAvstemmingId.copy(
                avstemming = AvstemmingDTO()
        ))

        println(oppdragState.toString())
        println("sender oppdrag")
        mqSender.sendOppdrag(oppdragState.toOppdrag())
        println("venter 10 sek på respons")
        Thread.sleep(10000)

        println("henter ut oppdrag, forhåpentligvis med respons")
        val oppdragStateHopefullyWithResponse = service.fetchOppdragStateById(oppdragState.id!!)
        val oppdragListe = listOf(oppdragStateHopefullyWithResponse)

        val mapper = AvstemmingMapper(oppdragListe, FagOmraadekode.SYKEPENGER_REFUSJON)
        val meldinger = mapper.lagAvstemmingsMeldinger()

        meldinger.forEach{
            val xmlMelding = JAXBAvstemmingsdata().fromAvstemmingsdataToXml(it)
            println("sender:")
            println(xmlMelding)
            mqAvstemmingSender.sendAvstemmingsmelding(it)
        }

    }
}