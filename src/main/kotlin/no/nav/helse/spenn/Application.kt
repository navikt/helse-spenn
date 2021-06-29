package no.nav.helse.spenn

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.avstemming.Avstemming
import no.nav.helse.spenn.avstemming.AvstemmingDao
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Simuleringer
import no.nav.helse.spenn.utbetaling.Kvitteringer
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.Transaksjoner
import no.nav.helse.spenn.utbetaling.Utbetalinger
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import javax.jms.Connection

fun main() {
    val env = System.getenv()
    if ("true" == env["CRON_JOB_MODE"]?.toLowerCase()) return avstemmingJob(env)
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val serviceAccountUserName = "/var/run/secrets/nais.io/service_user/username".readFile()
        ?: throw IllegalArgumentException("Forventer username")
    val serviceAccountPassword = "/var/run/secrets/nais.io/service_user/password".readFile()
        ?: throw IllegalArgumentException("Forventer passord")

    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.getValue("SIMULERING_SERVICE_URL"),
        stsSoapUrl = env.getValue("SECURITYTOKENSERVICE_URL"),
        username = serviceAccountUserName,
        password = serviceAccountPassword,
        disableCNCheck = true
    )

    val simuleringService = SimuleringService(simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()))

    val jmsConnection: Connection = mqConnection(env, serviceAccountUserName, serviceAccountPassword)

    val tilOppdragQueue = env.getValue("OPPDRAG_QUEUE_SEND")
    val svarFraOppdragQueue = env.getValue("OPPDRAG_QUEUE_MOTTAK")

    val jms = Jms(
        jmsConnection,
        tilOppdragQueue,
        svarFraOppdragQueue
    )
    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, simuleringService, jms, dataSourceBuilder)
    rapid.start()
}

fun isProd(env: Map<String, String>) = (env.getValue("NAIS_CLUSTER_NAME").startsWith("prod"))

fun rapidApp(
    rapid: RapidsConnection,
    simuleringService: SimuleringService,
    kø: Kø,
    database: Database
) {
    val dataSource = database.getDataSource()
    val oppdragDao = OppdragDao(dataSource)
    val log = LoggerFactory.getLogger(RapidsConnection::class.java)


    rapid.apply {
        Simuleringer(this, simuleringService)
        Utbetalinger(
            this,
            oppdragDao,
            kø.sendSession()
        )
        Kvitteringer(
            this,
            kø,
            oppdragDao
        )
        Transaksjoner(this, oppdragDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                database.migrate()
                kø.start()

                val objectMapper = jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                oppdragDao.hentBorkedOppdrag()?.also { (json, borkedOppdrag) ->
                    val packet = objectMapper.readTree(json)

                    val utbetalingslinjer = UtbetalingslinjerMapper(
                        packet["fødselsnummer"].asText(),
                        packet["organisasjonsnummer"].asText()
                    )
                        .fraBehov(packet["Utbetaling"])
                    log.info("Rekjører et feriepengeoppdrag")
                    borkedOppdrag.sendOppdrag(oppdragDao, utbetalingslinjer, Instant.now(), kø.sendSession())
                    log.info("Rekjøring av feriepengeoppdrag er utført")
                }
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                kø.close()
            }
        })
    }
}

private fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.Spenn")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }

    val serviceAccountUserName = "/var/run/secrets/nais.io/service_user/username".readFile()
        ?: throw IllegalArgumentException("Forventer username")
    val serviceAccountPassword = "/var/run/secrets/nais.io/service_user/password".readFile()
        ?: throw IllegalArgumentException("Forventer password")
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()
    val kafkaConfig = no.nav.helse.spenn.avstemming.KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        username = serviceAccountUserName,
        password = serviceAccountPassword,
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env["KAFKA_KEYSTORE_PATH"],
        keystorePassword = env["KAFKA_CREDSTORE_PASSWORD"],
    )
    val strings = StringSerializer()

    KafkaProducer(kafkaConfig.producerConfig(), strings, strings).use { producer ->
        mqConnection(env, serviceAccountUserName, serviceAccountPassword).use { jmsConnection ->
            jmsConnection.start()

            val dagen = LocalDate.now().minusDays(1)
            Avstemming(
                JmsUtSesjon(
                    jmsConnection,
                    env.getValue("AVSTEMMING_QUEUE_SEND"),
                ),
                producer,
                env.getValue("KAFKA_RAPID_TOPIC"),
                OppdragDao(dataSource),
                AvstemmingDao(dataSource),
            ).avstem(dagen)

            log.info("avstemming utført dagen=$dagen")
        }
        producer.flush()
    }
}

private fun mqConnection(env: Map<String, String>, mqUserName: String, mqPassword: String) =
    MQConnectionFactory().apply {
        hostName = env.getValue("MQ_HOSTNAME")
        port = env.getValue("MQ_PORT").toInt()
        channel = env.getValue("MQ_CHANNEL")
        queueManager = env.getValue("MQ_QUEUE_MANAGER")
        transportType = WMQConstants.WMQ_CM_CLIENT
        setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
    }.createConnection(mqUserName, mqPassword)

private fun String.readFile() = try {
    File(this).readText(Charsets.UTF_8)
} catch (err: Exception) {
    null
}
