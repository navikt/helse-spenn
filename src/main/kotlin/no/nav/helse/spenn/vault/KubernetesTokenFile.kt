package no.nav.helse.spenn.vault

import org.slf4j.LoggerFactory
import org.springframework.vault.authentication.KubernetesJwtSupplier

import java.io.IOException
import java.nio.charset.StandardCharsets

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import org.springframework.util.StreamUtils
import org.springframework.vault.VaultException
import org.springframework.vault.authentication.KubernetesServiceAccountTokenFile


class KubernetesTokenFile(private val resource: Resource) : KubernetesJwtSupplier {

    @JvmOverloads
    constructor(path: String = DEFAULT_KUBERNETES_SERVICE_ACCOUNT_TOKEN_FILE) : this(FileSystemResource(path))

    init {
        Assert.isTrue(resource.exists()) { String.format("Resource %s does not exist", resource) }
    }

    override fun get(): String {

        try {
            return String(readToken(this.resource), StandardCharsets.US_ASCII)
        } catch (e: IOException) {
            throw VaultException(String.format(
                    "Kube JWT token retrieval from %s failed", this.resource), e)
        }

    }

    companion object {

        private val LOG = LoggerFactory.getLogger(KubernetesTokenFile::class.java)

        val DEFAULT_KUBERNETES_SERVICE_ACCOUNT_TOKEN_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/token"

        @Throws(IOException::class)
        private fun readToken(resource: Resource): ByteArray {

            LOG.info("Reading vault token")
            Assert.notNull(resource, "Resource must not be null")
            resource.inputStream.use { `is` -> return StreamUtils.copyToByteArray(`is`) }
        }
    }
}
