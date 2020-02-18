package no.nav.helse.spenn.blackbox.metatest

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.spenn.ServiceUser
import no.nav.helse.spenn.blackbox.soap.SoapMock
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import no.nav.helse.spenn.oppdrag.UtbetalingsLinje
import no.nav.helse.spenn.oppdrag.dao.lagPåSidenSimuleringsrequest
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.vedtak.SpennOppdragFactory
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SoapMockSimuleringTest {

    private val soapMock = SoapMock(exposeTestContainerPorts = false)

    private lateinit var simuleringService: SimuleringService

    @BeforeAll
    fun setup() {
        println(soapMock.keystorePath.toAbsolutePath().toString())
        System.setProperty("javax.net.ssl.trustStore", soapMock.keystorePath.toAbsolutePath().toString())
        System.setProperty("javax.net.ssl.trustStorePassword", soapMock.keystorePassword)

        soapMock.start()

        val simuleringConfig = SimuleringConfig(
            simuleringServiceUrl = "https://localhost:${soapMock.httpsPort}/ws/simulering",
            stsSoapUrl = "https://localhost:${soapMock.httpsPort}/ws/SecurityTokenService",
            serviceUser = ServiceUser("hackerman", "password123")
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
        val utbetalingTemplate =
            SpennOppdragFactory.lagOppdragFraBehov(etEnkeltBehov(maksdato = maksDato), "12121212345")
        val utbetaling = utbetalingTemplate.copy(
            utbetaling = utbetalingTemplate.utbetaling!!.copy(
                utbetalingsLinjer = listOf(enOppdragsLinje)
            )
        )

        val result = simuleringService.simulerOppdrag(utbetaling.lagPåSidenSimuleringsrequest())
        assertNotNull(result, "simuleringresult skal ha blitt satt")
        assertEquals(SimuleringStatus.OK, result.status)
    }

}
