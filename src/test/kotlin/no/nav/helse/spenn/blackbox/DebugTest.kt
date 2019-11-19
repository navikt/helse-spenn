package no.nav.helse.spenn.blackbox

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class DebugTest {


    @Test
    fun test2() {

        setEnv("HEIHEI", "dette er en test")

        println(System.getenv("HEIHEI"))


    }


    @Test
    fun test1() {
        val network = Network.newNetwork()

        val vault = setupVault(network)
        //vault.withExposedPorts(8200)

        //vault.withPortBindings(PortBinding(Ports.Binding.bindPort(9092), ExposedPort(9092)))
        //vault.with
        vault.portBindings = listOf("8200:8200")
        //vault.withNetworkMode("host")
        //vault.withNetwork(Network.SHARED)

        vault.start()

        println("---------------------------------")
        //println(vault.getMappedPort(8200))

        println(vault.portBindings.toString())


        println("---------------------------------")



        Thread.sleep(60000)

    }

    companion object {

        private class DockerContainer(dockerImageName: String) : GenericContainer<DockerContainer>(dockerImageName)

    }

    private fun setupVault(network: Network): DebugTest.Companion.DockerContainer {
        return DebugTest.Companion.DockerContainer("vault:1.1.0")
                //.withNetwork(network)
                //.withNetworkAliases("localhost")
                .withEnv("VAULT_ADDR", "http://127.0.0.1:8200")
                .withEnv("VAULT_TOKEN", "token123")
                .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "token123")
                .withLogConsumer { print("vault: " + it.utf8String) }
    }

}