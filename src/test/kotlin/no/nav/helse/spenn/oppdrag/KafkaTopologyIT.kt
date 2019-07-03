package no.nav.helse.spenn.oppdrag

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import no.nav.helse.spenn.AppConfig
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.*
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.apache.commons.io.IOUtils
import java.util.*
import java.util.function.Consumer
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import org.springframework.boot.test.mock.mockito.MockBeans
import javax.annotation.PostConstruct

import kotlin.test.fail

@SpringBootTest(properties = [
    "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
    "SECURITY_TOKEN_SERVICE_REST_URL=localhost:8080",
    "SECURITYTOKENSERVICE_URL=localhost:8888",
    "SIMULERING_SERVICE_URL=localhost:9110",
    "STS_REST_USERNAME=foo",
    "STS_REST_PASSWORD=bar",
    "KAFKA_USERNAME=foo",
    "KAFKA_PASSWORD=bar",
    "STS_SOAP_USERNAME=foo",
    "STS_SOAP_PASSWORD=bar",
    "NAV_TRUSTSTORE_PATH=somewhere",
    "NAV_TRUSTSTORE_PASSWORD=somekey",
    "PLAIN_TEXT_KAFKA=true"])
@MockBean(classes= [SimuleringService::class, UtbetalingService::class])
class KafkaTopologyIT {

    companion object {
        var portBinding = Consumer<CreateContainerCmd> { e -> e.withPortBindings(PortBinding(Ports.Binding.bindPort(9092), ExposedPort(9093)))}
        val kafka = KafkaContainer("5.1.0")
                .withCreateContainerCmdModifier(portBinding)
        fun createTopic() {
            val createTopic = String.format("/usr/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic %s", VEDTAK_SYKEPENGER.name)
            execInContainer(createTopic)
        }
        fun createMessage(key: String, jsonMessage: String) {
            val message = String.format("echo '%s:%s'|/usr/bin/kafka-console-producer --broker-list localhost:9092 --topic %s --property \"parse.key=true\" --property \"key.separator=:\"",
                    key, jsonMessage.replace("\n",""), VEDTAK_SYKEPENGER.name)
            println(message)
            execInContainer(message)
        }

        fun execInContainer(message: String) {
            try {
                val execResult = kafka.execInContainer("/bin/sh", "-c", message)
                println(execResult.stderr)
                println(execResult.stdout)
                if (execResult.exitCode != 0) fail()
            } catch (e: Exception) {
                e.printStackTrace()
                fail()
            }
        }

        init {
            kafka.start()
            val node = IOUtils.toString(KafkaTopologyIT::class.java.getResource("/et_vedtak.json"), "UTF-8")
            createTopic()
            val key = UUID.randomUUID().toString()
            createMessage(key, node)
            // testing duplicate
            createMessage(key, node)
        }
    }


    @Test
    fun testSpennTopology() {

    }
}

