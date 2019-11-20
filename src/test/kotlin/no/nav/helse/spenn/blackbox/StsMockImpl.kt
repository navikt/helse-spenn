package no.nav.helse.spenn.blackbox

import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType
import javax.jws.*
import javax.jws.soap.SOAPBinding
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.ws.Action

@WebService(targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512?wsdl", name = "SecurityTokenService")
@XmlSeeAlso(
    org.apache.cxf.ws.security.sts.provider.model.ObjectFactory::class,
    org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory::class,
    org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory::class,
    org.apache.cxf.ws.security.sts.provider.model.utility.ObjectFactory::class,
    org.apache.cxf.ws.security.sts.provider.model.xmldsig.ObjectFactory::class,
    org.apache.cxf.ws.addressing.ObjectFactory::class
)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
class StsMockImpl(private val stsResponseGenerator: STSResponseGenerator) {
    @WebResult(
        name = "RequestSecurityTokenResponseCollection",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "responseCollection"
    )
    @Action(
        input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue",
        output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal"
    )
    @WebMethod(operationName = "Issue", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
    fun issueCollection(
        @WebParam(
            partName = "request",
            name = "RequestSecurityToken",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) request: RequestSecurityTokenType
    ): RequestSecurityTokenResponseCollectionType =
        stsResponseGenerator.buildRequestSecurityTokenResponseCollectionType(request)


    @WebResult(
        name = "RequestSecurityTokenResponse",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "response"
    )
    @Action(
        input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate",
        output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/ValidateFinal"
    )
    @WebMethod(operationName = "Validate", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate")
    fun validate(
        @WebParam(
            partName = "request",
            name = "RequestSecurityToken",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) request: RequestSecurityTokenType): RequestSecurityTokenResponseType =
        stsResponseGenerator.buildRequestSecurityTokenResponseType(request)

    @WebResult(
        name = "RequestSecurityTokenResponse",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "response"
    )
    @Action(
        input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/KET",
        output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/KETFinal"
    )
    @WebMethod(operationName = "KeyExchangeToken", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/KET")
    fun keyExchangeToken(
        @WebParam(
            partName = "request",
            name = "RequestSecurityToken",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) request: RequestSecurityTokenType): RequestSecurityTokenResponseType =
        stsResponseGenerator.buildRequestSecurityTokenResponseType(request)

    @WebResult(
        name = "RequestSecurityTokenResponse",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "response"
    )
    @Action(
        input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Renew",
        output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/RenewFinal"
    )
    @WebMethod(operationName = "Renew", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Renew")
    fun renew(
        @WebParam(
            partName = "request",
            name = "RequestSecurityToken",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) request: RequestSecurityTokenType): RequestSecurityTokenResponseType =
        stsResponseGenerator.buildRequestSecurityTokenResponseType(request)

    @WebResult(
        name = "RequestSecurityTokenResponseCollection",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "responseCollection"
    )
    @WebMethod(operationName = "RequestCollection")
    fun requestCollection(
        @WebParam(
            partName = "requestCollection",
            name = "RequestSecurityTokenCollection",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) requestCollection: RequestSecurityTokenCollectionType): RequestSecurityTokenResponseCollectionType =
        stsResponseGenerator.buildRequestSecurityTokenResponseCollectionType(requestCollection)

    /*
    @WebResult(
        name = "RequestSecurityTokenResponse",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
        partName = "response"
    )
    @Action(
        input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue",
        output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal"
    )
    @WebMethod(action = "Issue")
    fun issueSingle(
        @WebParam(
            partName = "request",
            name = "RequestSecurityToken",
            targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
        ) request: RequestSecurityTokenType): RequestSecurityTokenResponseType {
        println("---ISSUE SINGLE---")
        val ret = stsResponseGenerator.buildRequestSecurityTokenResponseType(request)
        println(ret)
        return ret
    }*/
}