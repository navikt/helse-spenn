package no.nav.helse.spenn.blackbox

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.apache.cxf.BusFactory
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.testcontainers.Testcontainers
import java.net.ServerSocket
import javax.xml.ws.Endpoint

class SoapMock : AutoCloseable {
    val port = ServerSocket(0).use { it.localPort }

    val simuleringMock = object : SimulerFpService {
        override fun sendInnOppdrag(parameters: SendInnOppdragRequest?): SendInnOppdragResponse {
            error("Not implemented.")
        }

        override fun simulerBeregning(parameters: SimulerBeregningRequest?): SimulerBeregningResponse {
            println(defaultObjectMapper.writeValueAsString(parameters))
            return SimulerBeregningResponse()
        }

    }

    private val server = Server(port)

    fun start() {
        Testcontainers.exposeHostPorts(port)
        val soapServlet = CXFNonSpringServlet()
        val servletHandler = ServletContextHandler()
        servletHandler.addServlet(ServletHolder(soapServlet), "/ws/*")

        server.handler = servletHandler
        server.start()

        BusFactory.setDefaultBus(soapServlet.bus)
        Endpoint.publish("/simulering", simuleringMock)
    }

    override fun close() {
        server.stop()
    }
}