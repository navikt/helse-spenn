package no.nav.helse.spenn.blackbox

import org.apache.cxf.security.SecurityContext
import org.apache.cxf.sts.StaticSTSProperties
import org.apache.cxf.sts.operation.TokenIssueOperation
import org.apache.cxf.sts.request.ReceivedToken
import org.apache.cxf.sts.request.RequestRequirements
import org.apache.cxf.sts.token.delegation.UsernameTokenDelegationHandler
import org.apache.cxf.sts.token.provider.SAMLTokenProvider
import org.apache.cxf.sts.token.provider.TokenProviderParameters
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType
import org.apache.wss4j.common.crypto.CryptoFactory
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.common.principal.CustomTokenPrincipal
import org.apache.wss4j.dom.engine.WSSConfig
import java.security.Principal
import java.util.*
import javax.security.auth.callback.CallbackHandler



class STSResponseGenerator(
    val keystoreKeyAlias: String,
    val keystorePath: String,
    val keystorePassword: String
) {
    private val encryptionProperties = Properties().also {
        WSSConfig.init()
        it["org.apache.wss4j.crypto.provider"] = "org.apache.wss4j.common.crypto.Merlin"
        it["org.apache.wss4j.crypto.merlin.keystore.password"] = keystorePassword
        it["org.apache.wss4j.crypto.merlin.keystore.file"] = keystorePath
    }

    val passwordCallbackHandler = CallbackHandler { callbacks ->
        callbacks
            .filterIsInstance<WSPasswordCallback>()
            .filter { it.identifier == keystoreKeyAlias }
            .forEach { it.password = keystorePassword }
    }

    val issueOperation = object : TokenIssueOperation() {
        override fun createTokenProviderParameters(
            requestRequirements: RequestRequirements,
            principal: Principal,
            messageContext: Map<String, Any>
        ): TokenProviderParameters = super.createTokenProviderParameters(requestRequirements, principal, messageContext).apply {
            tokenRequirements.onBehalfOf = null
            tokenRequirements.actAs = null
        }
    }.apply {
        val crypto = CryptoFactory.getInstance(encryptionProperties)

        setStsProperties(StaticSTSProperties().apply {
            encryptionCrypto = crypto
            signatureCrypto = crypto
            callbackHandler = passwordCallbackHandler
            signatureUsername = keystoreKeyAlias
            issuer = "spoof"
        })

        // Add Token Provider
        tokenProviders = listOf(SAMLTokenProvider())

        // Add TokenDelegationHandler for onBehalfOf
        delegationHandlers = listOf(
            object : UsernameTokenDelegationHandler() {
                override fun canHandleToken(delegateTarget: ReceivedToken): Boolean {
                    return true
                }
            }
        )
    }

    private val USERNAME = "localhost"

    private fun createSecurityContext(principal: Principal): SecurityContext {
        return object : SecurityContext {
            override fun getUserPrincipal() = principal
            override fun isUserInRole(role: String) = false
        }
    }

    private fun createMessageContext(principal: Principal): Map<String, Any> {
        val messageContext = HashMap<String, Any>()
        messageContext[SecurityContext::class.java.name] = createSecurityContext(principal)
        return messageContext
    }

    fun buildRequestSecurityTokenResponseCollectionType(requestCollection: RequestSecurityTokenCollectionType): RequestSecurityTokenResponseCollectionType {
        val principal = CustomTokenPrincipal(USERNAME)
        val messageContext = createMessageContext(principal)
        return issueOperation.issue(requestCollection, principal, messageContext)
    }

    /** Issue a token as part of collection  */
    fun buildRequestSecurityTokenResponseCollectionType(request: RequestSecurityTokenType): RequestSecurityTokenResponseCollectionType {
        val principal = CustomTokenPrincipal(USERNAME)
        val messageContext = createMessageContext(principal)
        return issueOperation.issue(request, principal, messageContext)
    }

    /** Issue a single token  */
    fun buildRequestSecurityTokenResponseType(request: RequestSecurityTokenType): RequestSecurityTokenResponseType {
        val principal = CustomTokenPrincipal(USERNAME)
        val messageContext = createMessageContext(principal)
        return issueOperation.issueSingle(request, principal, messageContext)
    }

}