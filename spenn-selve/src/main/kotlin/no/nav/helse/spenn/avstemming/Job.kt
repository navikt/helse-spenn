package no.nav.helse.spenn.avstemming

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import no.nav.helse.spenn.DataSourceBuilder
import no.nav.helse.spenn.JmsUtSesjon
import no.nav.helse.spenn.mqConnection
import no.nav.helse.spenn.readFile
import no.nav.helse.spenn.utbetaling.OppdragDao
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate


private val avstemmingsTid = Gauge.build("no/nav/virksomhet/tjenester/avstemming", "tidpusnkt for siste vellykkede avstemming").register()

private fun datasource(env: Map<String, String>): HikariDataSource {
    val prefix = "DATABASE_SPENN_AVSTEMMING"
    val gcpProjectId = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "gcp project id must be set" }
    val databaseRegion = requireNotNull(env["DATABASE_REGION"]) { "database region must be set" }
    val databaseInstance = requireNotNull(env["DATABASE_INSTANCE"]) { "database instance must be set" }
    val databaseUsername = requireNotNull(env["${prefix}_USERNAME"]) { "database username must be set" }
    val databasePassword = requireNotNull(env["${prefix}_PASSWORD"]) { "database password must be set"}
    val databaseName = requireNotNull(env["${prefix}_DATABASE"]) { "database name must be set"}
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        username = databaseUsername
        password = databasePassword

        maximumPoolSize = 1
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
    }
    return HikariDataSource(hikariConfig)
}

internal fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.Spenn")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }

    val serviceAccountUserName = env["SERVICEUSER_NAME"] ?: "/var/run/secrets/nais.io/service_user/username".readFile()
    val serviceAccountPassword = env["SERVICEUSER_PASSWORD"] ?: "/var/run/secrets/nais.io/service_user/password".readFile()
    val dataSource = if (env.containsKey("DATABASE_SPENN_AVSTEMMING_DATABASE"))
        datasource(env)
    else
        DataSourceBuilder(env).getDataSource()

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
        avstemmingsTid.setToCurrentTime()
        PushGateway("nais-prometheus-pushgateway.nais:9091").push(CollectorRegistry.defaultRegistry, "spenn_avstemming")
    }
}
