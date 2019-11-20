package no.nav.helse.spenn.blackbox

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.time.ZonedDateTime
import java.util.*

fun generateKeystore(
    alias: String,
    password: String
) : KeyStore = KeyStore.getInstance("pkcs12").apply {
    load(null)

    val keygen = KeyPairGenerator.getInstance("RSA")
    val keyPair = keygen.genKeyPair()
    setKeyEntry(alias, keyPair.private, password.toCharArray(),
        arrayOf(generateCertificate(keyPair, "CN=localhost")))
}

fun generateCertificate(keyPair: KeyPair, subject: String): Certificate {
    val dnName = X500Name(subject)
    val serial = "2458907890".toBigInteger()

    val startDate = ZonedDateTime.now().minusDays(1)
    val validFrom  = Date.from(startDate.toInstant())
    val validUntil = Date.from(startDate.plusYears(1).toInstant())
    val signatureAlgorithm = "SHA256WithRSA"

    val contentSigner = JcaContentSignerBuilder(signatureAlgorithm)
        .build(keyPair.private)

    val certificateHolder = JcaX509v3CertificateBuilder(dnName, serial, validFrom, validUntil, dnName, keyPair.public)
        .addExtension(ASN1ObjectIdentifier("2.5.29.19"), true, BasicConstraints(true))
        .build(contentSigner)

    return JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(certificateHolder)
}