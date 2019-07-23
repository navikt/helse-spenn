package no.nav.helse.spenn.vault


import org.springframework.cloud.vault.config.VaultProperties
import java.net.URI

import org.springframework.util.StringUtils
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.support.SslConfiguration
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration

internal object VaultConfigurationUtil {

    fun createSslConfiguration(ssl: VaultProperties.Ssl?): SslConfiguration {

        if (ssl == null) {
            return SslConfiguration.unconfigured()
        }

        var keyStore = KeyStoreConfiguration.unconfigured()
        var trustStore = KeyStoreConfiguration.unconfigured()

        if (ssl.keyStore != null) {
            if (StringUtils.hasText(ssl.keyStorePassword)) {
                keyStore = KeyStoreConfiguration.of(ssl.keyStore!!,
                        ssl.keyStorePassword!!.toCharArray())
            } else {
                keyStore = KeyStoreConfiguration.of(ssl.keyStore!!)
            }
        }

        if (ssl.trustStore != null) {

            if (StringUtils.hasText(ssl.trustStorePassword)) {
                trustStore = KeyStoreConfiguration.of(ssl.trustStore!!,
                        ssl.trustStorePassword!!.toCharArray())
            } else {
                trustStore = KeyStoreConfiguration.of(ssl.trustStore!!)
            }
        }

        return SslConfiguration(keyStore, trustStore)
    }

    fun createVaultEndpoint(vaultProperties: VaultProperties): VaultEndpoint {

        if (StringUtils.hasText(vaultProperties.uri)) {
            return VaultEndpoint.from(URI.create(vaultProperties.uri))
        }

        val vaultEndpoint = VaultEndpoint()
        vaultEndpoint.host = vaultProperties.host
        vaultEndpoint.port = vaultProperties.port
        vaultEndpoint.scheme = vaultProperties.scheme

        return vaultEndpoint
    }

}
