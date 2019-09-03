package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.vedtak.Vedtak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import java.util.*

class SimuleringControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider()
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }

    }

}

