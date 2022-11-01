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
import no.nav.helse.spenn.avstemming.avstemmingJob
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Simuleringer
import no.nav.helse.spenn.utbetaling.Kvitteringer
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.Transaksjoner
import no.nav.helse.spenn.utbetaling.Utbetalinger
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import javax.jms.Connection

fun main() {
    val env = System.getenv()
    if ("true" == env["CRON_JOB_MODE"]?.lowercase()) return avstemmingJob(env)
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val serviceAccountUserName = env["SERVICEUSER_NAME"] ?: "/var/run/secrets/nais.io/service_user/username".readFile()
    val serviceAccountPassword = env["SERVICEUSER_PASSWORD"] ?: "/var/run/secrets/nais.io/service_user/password".readFile()

    val clusterName = env["NAIS_CLUSTER_NAME"]

    val simuleringService =  if (clusterName == null || clusterName in setOf("prod-fss", "prod-gcp")) {
        val simuleringConfig = SimuleringConfig(
            simuleringServiceUrl = env.getValue("SIMULERING_SERVICE_URL"),
            stsSoapUrl = env.getValue("SECURITYTOKENSERVICE_URL"),
            username = serviceAccountUserName,
            password = serviceAccountPassword,
            disableCNCheck = true
        )
        SimuleringService(simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()))
    } else null

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

fun rapidApp(
    rapid: RapidsConnection,
    simuleringService: SimuleringService?,
    kø: Kø,
    database: Database
) {
    val dataSource = database.getDataSource()
    val oppdragDao = OppdragDao(dataSource)

    rapid.apply {
        if (simuleringService != null) {
            Simuleringer(this, simuleringService)
        }
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
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                kø.close()
            }
        })
    }
}

internal fun mqConnection(env: Map<String, String>, mqUserName: String, mqPassword: String) =
    MQConnectionFactory().apply {
        hostName = env.getValue("MQ_HOSTNAME")
        port = env.getValue("MQ_PORT").toInt()
        channel = env.getValue("MQ_CHANNEL")
        queueManager = env.getValue("MQ_QUEUE_MANAGER")
        transportType = WMQConstants.WMQ_CM_CLIENT
        setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
    }.createConnection(mqUserName, mqPassword)

fun String.readFile() = File(this).readText(Charsets.UTF_8)
