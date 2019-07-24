package no.nav.helse.spenn.vault



import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.vault.config.VaultBootstrapConfiguration
import org.springframework.cloud.vault.config.VaultProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.Assert
import org.springframework.vault.authentication.ClientAuthentication
import org.springframework.vault.authentication.KubernetesAuthentication
import org.springframework.vault.authentication.KubernetesAuthenticationOptions
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.SimpleVaultEndpointProvider
import org.springframework.vault.client.VaultClients
import org.springframework.vault.client.VaultEndpointProvider
import org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper
import org.springframework.vault.config.ClientHttpRequestFactoryFactory
import org.springframework.vault.support.ClientOptions
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@AutoConfigureBefore(value = [VaultBootstrapConfiguration::class])
@ConditionalOnProperty(name = ["spring.cloud.vault.enabled"], matchIfMissing = true)
@EnableConfigurationProperties(VaultProperties::class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class VaultConfiguration(private val applicationContext: ConfigurableApplicationContext,
                                  private val vaultProperties: VaultProperties,
                                  endpointProvider: ObjectProvider<VaultEndpointProvider>) : InitializingBean {

    companion object {
        private  val LOG = LoggerFactory.getLogger(VaultConfiguration::class.java)
    }

    private val endpointProvider: VaultEndpointProvider


    private var restOperations: RestOperations? = null

    private var externalRestOperations: RestOperations? = null

    init {

        var provider = endpointProvider.ifAvailable

        if (provider == null) {
            provider = SimpleVaultEndpointProvider
                    .of(VaultConfigurationUtil.createVaultEndpoint(vaultProperties))
        }

        this.endpointProvider = provider
    }

    override fun afterPropertiesSet() {

        val clientHttpRequestFactory = clientHttpRequestFactoryWrapper()
                .clientHttpRequestFactory

        this.restOperations = VaultClients.createRestTemplate(this.endpointProvider,
                clientHttpRequestFactory)

        this.externalRestOperations = RestTemplate(clientHttpRequestFactory)
    }

    @Bean
    @ConditionalOnMissingBean
    fun clientHttpRequestFactoryWrapper(): ClientFactoryWrapper {

        val clientOptions = ClientOptions(
                Duration.ofMillis(this.vaultProperties.connectionTimeout.toLong()),
                Duration.ofMillis(this.vaultProperties.readTimeout.toLong()))

        val sslConfiguration = VaultConfigurationUtil
                .createSslConfiguration(this.vaultProperties.ssl)

        return ClientFactoryWrapper(
                ClientHttpRequestFactoryFactory.create(clientOptions, sslConfiguration))
    }

    @Bean
    @Primary
    fun clientAuthentication(): ClientAuthentication {

        return when (this.vaultProperties.authentication) {

            VaultProperties.AuthenticationMethod.KUBERNETES -> {
                LOG.info("using kubernetes service token for client authentication")
                kubernetesAuthentication()
            }

            VaultProperties.AuthenticationMethod.TOKEN -> {
                LOG.info("using vault token for client authentication")
                Assert.hasText(this.vaultProperties.token,
                        "Token (spring.cloud.vault.token) must not be empty")
                TokenAuthentication(this.vaultProperties.token!!)
            }
            else -> throw UnsupportedOperationException(
                    String.format("Client authentication %s not supported",
                            this.vaultProperties.authentication))
        }
    }

    private fun kubernetesAuthentication(): ClientAuthentication {

        val kubernetes = vaultProperties.kubernetes

        Assert.hasText(kubernetes.role,
                "Role (spring.cloud.vault.kubernetes.role) must not be empty")
        Assert.hasText(kubernetes.serviceAccountTokenFile,
                "Service account token file (spring.cloud.vault.kubernetes.service-account-token-file) must not be empty")

        val options = KubernetesAuthenticationOptions
                .builder().path(kubernetes.kubernetesPath).role(kubernetes.role)
                .jwtSupplier(KubernetesTokenFile(
                        kubernetes.serviceAccountTokenFile))
                .build()

        return KubernetesAuthentication(options, this.restOperations!!)
    }




}
