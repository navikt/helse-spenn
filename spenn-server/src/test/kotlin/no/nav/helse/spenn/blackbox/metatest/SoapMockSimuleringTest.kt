package no.nav.helse.spenn.blackbox.metatest

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.spenn.ServiceUser
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.blackbox.soap.SoapMock
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.Utbetalingsbehov
import no.nav.helse.spenn.oppdrag.dao.lagPåSidenSimuleringsrequest
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.toOppdragsbehov
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    fun `Test simulering med Soap og STS`() {
        val result = simuleringService.simulerOppdrag(Utbetalingsbehov(etEnkeltBehov(), "12121212345")
                .lagPåSidenSimuleringsrequest())
        assertNotNull(result, "simuleringresult skal ha blitt satt")
        assertEquals(SimuleringStatus.OK, result.status)
    }

}
