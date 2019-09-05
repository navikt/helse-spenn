package no.nav.helse.spenn.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spenn.defaultObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc

class SimuleringControllerTest {

//    @Autowired
//    lateinit var mockMvc: MockMvc
//
//    companion object {
//        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(33333))
//        @BeforeAll
//        @JvmStatic
//        fun before() {
//            server.start()
//            WireMock.configureFor(server.port())
//            stubOIDCProvider()
//        }
//        @AfterAll
//        @JvmStatic
//        fun after() {
//            server.stop()
//        }
//    }
//
//    @Test
//    fun runSimulering() {
//
//    }
}

val etVedtakJson = """{"soknadId":"9ada4305-1e45-4d48-ba48-a504bc96040d","aktorId":"en random aktørid","vedtaksperioder":[{"fom":"2020-01-15","tom":"2020-01-30","grad":100,"dagsats":1234,"fordeling":[{"mottager":"897654321","andel":100}]}],"maksDato":"2020-09-03"}"""