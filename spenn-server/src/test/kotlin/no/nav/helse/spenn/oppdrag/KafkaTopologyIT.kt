package no.nav.helse.spenn.oppdrag

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import no.nav.helse.spenn.vedtak.*
import org.apache.kafka.streams.KafkaStreams

import org.junit.jupiter.api.Test
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.apache.commons.io.IOUtils
import java.util.*
import java.util.function.Consumer
//import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

import kotlin.test.fail

class KafkaTopologyIT {

    //@Autowired
    lateinit var streamConsumer: StreamConsumer

    companion object {
        var portBinding = Consumer<CreateContainerCmd> { e -> e.withPortBindings(PortBinding(Ports.Binding.bindPort(9092), ExposedPort(9093)))}
        val kafka = KafkaContainer("5.1.0")
                .withCreateContainerCmdModifier(portBinding)
        fun createTopic() {
            val createTopic = String.format("/usr/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic %s", SYKEPENGER_BEHOV_TOPIC.name)
            execInContainer(createTopic)
        }
        fun createMessage(key: String, jsonMessage: String) {
            val message = String.format("echo '%s:%s'|/usr/bin/kafka-console-producer --broker-list localhost:9092 --topic %s --property \"parse.key=true\" --property \"key.separator=:\"",
                    key, jsonMessage.replace("\n",""), SYKEPENGER_BEHOV_TOPIC.name)
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
        assertEquals(KafkaStreams.State.RUNNING, streamConsumer.state(), "Kafka stream should be running")
    }
}

