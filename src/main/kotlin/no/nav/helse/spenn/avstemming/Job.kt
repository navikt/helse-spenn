package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.DataSourceBuilder
import no.nav.helse.spenn.JmsUtSesjon
import no.nav.helse.spenn.mqConnection
import no.nav.helse.spenn.readFile
import no.nav.helse.spenn.utbetaling.OppdragDao
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.Spenn")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }

    val serviceAccountUserName = "/var/run/secrets/nais.io/service_user/username".readFile()
    val serviceAccountPassword = "/var/run/secrets/nais.io/service_user/password".readFile()
    val dataSource = DataSourceBuilder(env).getDataSource()

    val kafkaConfig = KafkaConfig(
            bootstrapServers = env.getValue("KAFKA_BROKERS"),
            truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
            truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
            keystoreLocation = env["KAFKA_KEYSTORE_PATH"],
            keystorePassword = env["KAFKA_CREDSTORE_PASSWORD"],
    )

    val strings = StringSerializer()
    KafkaProducer(kafkaConfig.producerConfig(), strings, strings).use { producer ->
        mqConnection(env, serviceAccountUserName, serviceAccountPassword).use { jmsConnection ->
            jmsConnection.start()

            val igår = LocalDate.now().minusDays(1)
            Avstemming(
                    JmsUtSesjon(jmsConnection, env.getValue("AVSTEMMING_QUEUE_SEND")),
                    producer,
                    env.getValue("KAFKA_RAPID_TOPIC"),
                    OppdragDao(dataSource),
                    AvstemmingDao(dataSource),
            ).avstemTilOgMed(igår)

            log.info("avstemming utført for betalinger frem til og med $igår")
        }
        producer.flush()
    }
}
