package no.nav.helse.spenn.blackbox.soap

import no.nav.helse.spenn.blackbox.generateKeystore
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SendInnOppdragResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.ext.logging.LoggingInInterceptor
import org.apache.cxf.ext.logging.LoggingOutInterceptor
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.phase.Phase
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.wss4j.dom.WSConstants
import org.eclipse.jetty.server.*
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.testcontainers.Testcontainers
import java.net.ServerSocket
import java.nio.file.Files
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.namespace.QName
import javax.xml.ws.Endpoint
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse as SimulerBeregningResponseWrapper

class SoapMock(private val exposeTestContainerPorts:Boolean = true) : AutoCloseable {
    val httpPort = ServerSocket(0).use { it.localPort }
    val httpsPort = ServerSocket(0).use { it.localPort }
    val keystorePath = Files.createTempFile("keystore", ".p12")
    val keystorePassword = "changeit"
    val keystoreKeyAlias = "localhost"

    val simuleringMock = object : SimulerFpService {
        override fun sendInnOppdrag(parameters: SendInnOppdragRequest?): SendInnOppdragResponse {
            error("Not implemented.")
        }

        override fun simulerBeregning(req: SimulerBeregningRequest): SimulerBeregningResponseWrapper {
            println(defaultObjectMapper.writeValueAsString(req))
            return SimulerBeregningResponseWrapper().apply {
                response = SimulerBeregningResponse().apply {
                    simulering = Beregning().apply {
                        gjelderId = req.request.oppdrag.oppdragGjelderId
                        gjelderNavn = "OLA NORDMANN"
                        datoBeregnet = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        belop = 5000.toBigDecimal()
                        beregningsPeriode.add(BeregningsPeriode().apply {
                            this.periodeFom = req.request.simuleringsPeriode.datoSimulerFom
                            this.periodeTom = req.request.simuleringsPeriode.datoSimulerTom
                        })
                    }
                }
            }
        }

    }

    private val server = Server()
        .setupTLS()

    fun start() {
        val keystore = generateKeystore(keystoreKeyAlias, keystorePassword)
        Files.newOutputStream(keystorePath).use {
            keystore.store(it, keystorePassword.toCharArray())
        }

        if (exposeTestContainerPorts) {
            Testcontainers.exposeHostPorts(httpPort, httpsPort)
        }
        val soapServlet = CXFNonSpringServlet()
        val servletHandler = ServletContextHandler()
        servletHandler.addServlet(ServletHolder(soapServlet), "/ws/*")

        server.handler = servletHandler
        server.start()

        BusFactory.setDefaultBus(soapServlet.bus)

        val ignoreWssHeaderInterceptor = object : AbstractSoapInterceptor(Phase.PRE_PROTOCOL) {
            override fun handleMessage(p0: SoapMessage?) {}

            override fun getUnderstoodHeaders(): MutableSet<QName> = mutableSetOf(
                QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN),
                QName(WSConstants.WSSE11_NS, WSConstants.WSSE_LN)
            )
        }

        Endpoint.publish("/simulering", simuleringMock).also {
            it as EndpointImpl
            it.inInterceptors.add(LoggingInInterceptor())
            it.inInterceptors.add(ignoreWssHeaderInterceptor)
            it.outInterceptors.add(LoggingOutInterceptor())
        }
        Endpoint.publish("/SecurityTokenService", STSMockImpl(
            STSResponseGenerator(
                keystoreKeyAlias = keystoreKeyAlias,
                keystorePath = keystorePath.toAbsolutePath().toString(),
                keystorePassword = keystorePassword
            )
        )
        ).also {
            it as EndpointImpl
            it.inInterceptors.add(LoggingInInterceptor())
            it.inInterceptors.add(ignoreWssHeaderInterceptor)
            it.outInterceptors.add(LoggingOutInterceptor())
        }
    }

    override fun close() {
        server.stop()
    }

    fun Server.setupTLS() = apply {
        val httpConnector = ServerConnector(this).apply {
            port = httpPort
        }

        val httpsCfg = HttpConfiguration().apply {
            addCustomizer(SecureRequestCustomizer())
        }

        val sslContextFactory = SslContextFactory.Server().also {
            it.certAlias = keystoreKeyAlias
            it.keyStorePath = keystorePath.toAbsolutePath().toString()
            it.setKeyStorePassword(keystorePassword)
            it.setKeyManagerPassword(keystorePassword)
        }

        val httpsConnector = ServerConnector(
            server,
            SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
            HttpConnectionFactory(httpsCfg)
        ).apply {
            port = httpsPort
        }
        connectors = arrayOf(httpConnector, httpsConnector)
    }
}