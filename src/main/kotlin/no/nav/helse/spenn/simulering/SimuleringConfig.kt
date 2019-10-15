package no.nav.helse.spenn.simulering

import no.nav.system.os.eksponering.simulerfpservicewsbinding.SimulerFpService
import org.apache.cxf.Bus
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import javax.xml.namespace.QName
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.trust.STSClient
import org.apache.neethi.Policy
import org.slf4j.LoggerFactory

//@Configuration
class SimuleringConfig(/*@Value("\${SIMULERING_SERVICE_URL}")*/ val simuleringServiceUrl: String,
                       /*@Value("\${SECURITYTOKENSERVICE_URL}")*/ val stsUrl: String,
                       /*@Value("\${STS_SOAP_USERNAME}")*/ val stsUsername: String,
                       /*@Value("\${STS_SOAP_PASSWORD}")*/ val stsPassword: String) {
    private val WSDL = "wsdl/no/nav/system/os/eksponering/simulerFpServiceWSBinding.wsdl"
    private val NAMESPACE = "http://nav.no/system/os/eksponering/simulerFpServiceWSBinding"
    private val SERVICE = QName(NAMESPACE, "simulerFpService")
    private val PORT = QName(NAMESPACE, "simulerFpServicePort")
    private val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:untPolicy.xml"
    private val STS_SAML_POLICY = "classpath:requestSamlPolicy.xml"

    companion object {
        private val log = LoggerFactory.getLogger(SimuleringConfig::class.java)
    }

    //@Bean
    fun wrapWithSTSSimulerFpService(bus : Bus): SimulerFpService {
        log.info("using simuleringservice url ${simuleringServiceUrl}")
        val factory = JaxWsProxyFactoryBean().apply {
            address = simuleringServiceUrl
            wsdlURL = WSDL
            serviceName = SERVICE
            endpointName = PORT
            serviceClass = SimulerFpService::class.java
            features = listOf(WSAddressingFeature())

        }
        return factory.create(SimulerFpService::class.java).apply {
            val sts = STSClient(bus).apply {
                isEnableAppliesTo = false
                isAllowRenewing = false

                location = stsUrl
                properties = mapOf(
                        SecurityConstants.USERNAME to stsUsername,
                        SecurityConstants.PASSWORD to stsPassword
                )
                setPolicy(bus.resolvePolicy(STS_CLIENT_AUTHENTICATION_POLICY))
            }
            ClientProxy.getClient(this).apply {
                requestContext[SecurityConstants.STS_CLIENT] = sts
                requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = true
                setClientEndpointPolicy(bus.resolvePolicy(STS_SAML_POLICY))
            }
        }
    }

    private fun Bus.resolvePolicy(policyUri: String): Policy {
        val registry = getExtension(PolicyEngine::class.java).registry
        val resolved = registry.lookup(policyUri)

        val policyBuilder = getExtension(PolicyBuilder::class.java)
        val referenceResolver = RemoteReferenceResolver("", policyBuilder)

        return resolved ?: referenceResolver.resolveReference(policyUri)
    }

    private fun Client.setClientEndpointPolicy(policy: Policy) {
        val policyEngine: PolicyEngine = bus.getExtension(PolicyEngine::class.java)
        val message = SoapMessage(Soap12.getInstance())
        val endpointPolicy = policyEngine.getClientEndpointPolicy(endpoint.endpointInfo, null, message)
        policyEngine.setClientEndpointPolicy(endpoint.endpointInfo, endpointPolicy.updatePolicy(policy, message))
    }
}
