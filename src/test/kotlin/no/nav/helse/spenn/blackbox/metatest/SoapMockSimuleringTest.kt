package no.nav.helse.spenn.blackbox.metatest

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.spenn.blackbox.soap.SoapMock
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Status
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoapMockSimuleringTest {


    val soapMock = SoapMock()

    private lateinit var simuleringService: SimuleringService

    @BeforeAll
    fun setup() {
        println(soapMock.keystorePath.toAbsolutePath().toString())
        System.setProperty("javax.net.ssl.trustStore", soapMock.keystorePath.toAbsolutePath().toString())
        System.setProperty("javax.net.ssl.trustStorePassword", soapMock.keystorePassword)
        //System.setProperty("javax.net.debug", "all")
        //System.setProperty("java.net.ssl.keyStoreType", "PKCS12")

        soapMock.start()

        val simuleringConfig = SimuleringConfig(
            simuleringServiceUrl = "https://localhost:${soapMock.httpsPort}/ws/simulering",
            stsUrl = "https://localhost:${soapMock.httpsPort}/ws/SecurityTokenService",
            stsUsername = "hackerman",
            stsPassword = "password123"
        )

        simuleringService = SimuleringService(
            simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()),
            PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        )
    }

    @Test
    fun testSimuleringMedSoapOgSTS() {
        val maksDato = LocalDate.now().plusYears(1).minusDays(50)
        val vedtakFom = LocalDate.now().minusWeeks(2)
        val vedtakTom = LocalDate.now()
        val enOppdragsLinje = UtbetalingsLinje(
            id = "1234567890",
            datoFom = vedtakFom,
            datoTom = vedtakTom,
            sats = BigDecimal.valueOf(1230),
            satsTypeKode = SatsTypeKode.MÅNEDLIG,
            utbetalesTil = "123456789",
            grad = BigInteger.valueOf(100)
        )
        val utbetaling = UtbetalingsOppdrag(
            operasjon = AksjonsKode.SIMULERING,
            oppdragGjelder = "12121212345",
            utbetalingsLinje = listOf(enOppdragsLinje),
            behov = etEnkeltBehov(maksdato = maksDato)
        )
        val oppdragState = OppdragStateDTO(
            id = 1L,
            sakskompleksId = UUID.randomUUID(),
            utbetalingsreferanse = "1001",
            utbetalingsOppdrag = utbetaling
        )

        val nyDTO = simuleringService.runSimulering(oppdragState)
        assertNotNull(nyDTO.simuleringResult, "simuleringresult skal ha blitt satt")
        assertEquals(Status.OK, nyDTO.simuleringResult!!.status )

    }
}