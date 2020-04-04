package no.nav.helse.spenn

import com.ibm.mq.jms.MQConnectionFactory
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
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.util.*
import javax.jms.Connection

fun main() {
    val env = System.getenv()
    if ("true" == env["CRON_JOB_MODE"]?.toLowerCase()) return avstemmingJob(env)
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.getValue("SIMULERING_SERVICE_URL"),
        stsSoapUrl = env.getValue("SECURITYTOKENSERVICE_URL"),
        username = "/var/run/secrets/nais.io/service_user/username".readFile(),
        password = "/var/run/secrets/nais.io/service_user/password".readFile(),
        disableCNCheck = true
    )

    val simuleringService = SimuleringService(simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()))
    val oppdragDao = OppdragDao(dataSource)

    val jmsConnection: Connection = mqConnection(env)

    RapidApplication.create(env).apply {
        Simuleringer(this, simuleringService)
        Utbetalinger(
            this,
            jmsConnection,
            env.getValue("OPPDRAG_QUEUE_SEND"),
            env.getValue("OPPDRAG_QUEUE_MOTTAK"),
            oppdragDao
        )
        Kvitteringer(
            this,
            jmsConnection,
            env.getValue("OPPDRAG_QUEUE_MOTTAK"),
            oppdragDao
        )
        Transaksjoner(this, oppdragDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
                jmsConnection.start()
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                jmsConnection.close()
            }
        })
    }.start()
}

private fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.Spenn")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    mqConnection(env).use {
        it.start()

        val id = UUID.randomUUID()
        val dagen = LocalDate.now().minusDays(1)

        log.info("Starter avstemming id=$id dagen=$dagen")
        Avstemming(
            it,
            env.getValue("AVSTEMMING_QUEUE_SEND"),
            OppdragDao(dataSource),
            AvstemmingDao(dataSource)
        ).avstem(id, dagen)
        log.info("avstemming utført id=$id dagen=$dagen")
    }
}

private fun mqConnection(env: Map<String, String>) =
    MQConnectionFactory().apply {
        hostName = env.getValue("MQ_HOSTNAME")
        port = env.getValue("MQ_PORT").toInt()
        channel = env.getValue("MQ_CHANNEL")
        queueManager = env.getValue("MQ_QUEUE_MANAGER")
        transportType = WMQConstants.WMQ_CM_CLIENT
    }.createConnection(env.getValue("MQ_USERNAME"), env.getValue("MQ_PASSWORD"))

private fun String.readFile() = File(this).readText(Charsets.UTF_8)
