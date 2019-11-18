package no.nav.helse.spenn.blackbox

internal val sts_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Header>
        <wsa:MessageID>urn:uuid:5e22dd04-7a8e-494a-a05e-2fff16ecf883</wsa:MessageID>
        <wsa:Action>http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal</wsa:Action>
        <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
    </soapenv:Header>
    <soapenv:Body>
        <wst:RequestSecurityTokenResponseCollection xmlns:wst="http://docs.oasis-open.org/ws-sx/ws-trust/200512"
                                                    xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
            <wst:RequestSecurityTokenResponse Context="supportLater">
                <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>
                <wst:RequestedSecurityToken>
                    <saml2:Assertion Version="2.0" ID="SAML-8d11de08-b17f-45ba-bd18-68098a4d28ce" IssueInstant="2018-09-06T10:28:45Z"
                                     xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
                        <saml2:Issuer>{{issuer}}</saml2:Issuer>
                        <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
                            <SignedInfo>
                                <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                                <SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
                                <Reference URI="#SAML-8d11de08-b17f-45ba-bd18-68098a4d28ce">
                                    <Transforms>
                                        <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                                        <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                                    </Transforms>
                                    <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                                    <DigestValue>{{digest}}</DigestValue>
                                </Reference>
                            </SignedInfo>
                            <SignatureValue>{{signature}}</SignatureValue>
                            <KeyInfo>
                                <X509Data>
                                    <X509Certificate>{{certificate}}</X509Certificate>
                                    <X509IssuerSerial>
                                        <X509IssuerName>{{issuerName}}</X509IssuerName>
                                        <X509SerialNumber>2363879011200190627239759946745671848168525609</X509SerialNumber>
                                    </X509IssuerSerial>
                                </X509Data>
                            </KeyInfo>
                        </Signature>
                        <saml2:Subject>
                            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">{{username}}</saml2:NameID>
                            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                                <saml2:SubjectConfirmationData NotBefore="2018-09-06T10:28:42Z" NotOnOrAfter="2018-09-06T11:28:48Z"/>
                            </saml2:SubjectConfirmation>
                        </saml2:Subject>
                        <saml2:Conditions NotBefore="2018-09-06T10:28:42Z" NotOnOrAfter="2018-09-06T11:28:48Z"/>
                        <saml2:AttributeStatement>
                            <saml2:Attribute Name="identType" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>Systemressurs</saml2:AttributeValue>
                            </saml2:Attribute>
                            <saml2:Attribute Name="authenticationLevel" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>0</saml2:AttributeValue>
                            </saml2:Attribute>
                            <saml2:Attribute Name="consumerId" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>{{username}}</saml2:AttributeValue>
                            </saml2:Attribute>
                        </saml2:AttributeStatement>
                    </saml2:Assertion>
                </wst:RequestedSecurityToken>
                <wst:Lifetime>
                    <wsu:Created>2018-09-06T10:28:42Z</wsu:Created>
                    <wsu:Expires>2018-09-06T11:28:48Z</wsu:Expires>
                </wst:Lifetime>
            </wst:RequestSecurityTokenResponse>
        </wst:RequestSecurityTokenResponseCollection>
    </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()