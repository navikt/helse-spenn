package no.nav.helse.spenn.blackbox

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.phase.PhaseInterceptor
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.testcontainers.Testcontainers
import java.net.ServerSocket
import javax.xml.soap.SOAPMessage
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
        Endpoint.publish("/simulering", simuleringMock).also {
            it as EndpointImpl
            it.inInterceptors.add(object : PhaseInterceptor<SoapMessage> {
                override fun getBefore(): MutableSet<String> = mutableSetOf()

                override fun handleMessage(msg: SoapMessage) {
                    SAAJInInterceptor.INSTANCE.handleMessage(msg)
                    val soapMessage = msg.getContent(SOAPMessage::class.java)
                    soapMessage.soapHeader.removeContents()
                }

                override fun handleFault(p0: SoapMessage?) {

                }

                override fun getAdditionalInterceptors(): MutableCollection<PhaseInterceptor<out Message>> = mutableListOf()

                override fun getPhase(): String = Phase.PRE_PROTOCOL

                override fun getId(): String = javaClass.name

                override fun getAfter(): MutableSet<String> = mutableSetOf()

            })
        }
    }

    override fun close() {
        server.stop()
    }
}