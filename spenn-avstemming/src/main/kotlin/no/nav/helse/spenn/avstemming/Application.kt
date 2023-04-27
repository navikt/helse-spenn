package no.nav.helse.spenn.avstemming

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    rapidApp(System.getenv())
}

private fun rapidApp(env: Map<String, String>) {
    val rapid: RapidsConnection = RapidApplication.create(env)
    val dataSourceBuilder = DataSourceBuilder(env)
    mqConnection(env, env.getValue("SERVICEUSER_NAME"), env.getValue("SERVICEUSER_PASSWORD")).use { jmsConnection ->
        jmsConnection.start()
        val avstemmingkø = JmsUtSesjon(jmsConnection, env.getValue("AVSTEMMING_QUEUE_SEND"))
        rapidApp(rapid, dataSourceBuilder, avstemmingkø)
        rapid.start()
    }
}

fun rapidApp(rapid: RapidsConnection, database: Database, avstemmingkø: UtKø) {
    val oppdragDao = OppdragDao(database::getDataSource)
    val avstemmingDao = AvstemmingDao(database::getDataSource)

    rapid.apply {
        Avstemminger(this, oppdragDao, avstemmingDao, avstemmingkø)
        Utbetalinger(this, oppdragDao)
        Overføringer(this, oppdragDao)
        Transaksjoner(this, oppdragDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                database.migrate()
            }
        })
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
