package no.nav.helse.spenn.oppdrag



import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.testsupport.TestDb
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.Month
import java.util.*

class MQOppdragIT {

    var mqConn = MQConnectionFactory().apply {
        hostName = "localhost"
        port = 1414
        channel = "DEV.ADMIN.SVRCONN"
        queueManager = "QM1"
        transportType = WMQConstants.WMQ_CM_CLIENT
    }.createConnection("admin", "passw0rd")

    val mqSender = OppdragMQSender(
            mqConn,
            "DEV.QUEUE.1",
            "DEV.QUEUE.3",
            JAXBOppdrag()
    )

    val oppdragStateService = OppdragStateService(
            OppdragStateJooqRepository(TestDb.createMigratedDSLContext())
    )

    @Test
    fun sendOppdragTilOS() {
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
                oppdragGjelder = "21038014495", utbetalingsLinje = listOf(oppdragslinje1, oppdragslinje2, oppdragslinje3),
                behov = etEnkeltBehov()
        )

        val oppdragState = OppdragStateDTO(soknadId = UUID.randomUUID(),
                utbetalingsOppdrag = utbetaling, avstemming = AvstemmingDTO())
        oppdragStateService.saveOppdragState(oppdragState)
        mqSender.sendOppdrag(oppdragState.toOppdrag())
        //wait for answer from OS
        Thread.sleep(10000)
    }
}