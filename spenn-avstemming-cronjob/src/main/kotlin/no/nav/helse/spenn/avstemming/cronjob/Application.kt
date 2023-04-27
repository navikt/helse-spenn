package no.nav.helse.spenn.avstemming.cronjob

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun main() {
    val env = System.getenv()
    avstemmingJob(env)
}

private fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.avstemming.cronjob.App")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }

    val kafkaConfig = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env["KAFKA_KEYSTORE_PATH"],
        keystorePassword = env["KAFKA_CREDSTORE_PASSWORD"],
    )

    val strings = StringSerializer()
    KafkaProducer(kafkaConfig.producerConfig(), strings, strings).use { producer ->
        val igår = LocalDate.now().minusDays(1)
        producer.send(ProducerRecord(env.getValue("KAFKA_RAPID_TOPIC"), utførAvstemming(igår)))
        log.info("avstemming utført for betalinger frem til og med $igår")
        producer.flush()
    }
}

@Language("JSON")
private fun utførAvstemming(dagen: LocalDate) = """{
    "@event_name": "utfør_avstemming",
    "@id": "${UUID.randomUUID()}",
    "@opprettet": "${LocalDateTime.now()}",
    "dagen": "$dagen"
}"""
