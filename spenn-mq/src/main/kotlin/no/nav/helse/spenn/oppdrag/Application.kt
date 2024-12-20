package no.nav.helse.spenn.oppdrag

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.rapids_rivers.RapidApplication
import java.io.File
import javax.jms.Connection

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val serviceAccountUserName = env.getValue("SERVICEUSER_NAME")
    val serviceAccountPassword = env.getValue("SERVICEUSER_PASSWORD")

    val jmsConnection: Connection = mqConnection(env, serviceAccountUserName, serviceAccountPassword)

    val tilOppdragQueue = env.getValue("OPPDRAG_QUEUE_SEND")
    val svarFraOppdragQueue = env.getValue("OPPDRAG_QUEUE_MOTTAK")

    val jms = Jms(jmsConnection, tilOppdragQueue, svarFraOppdragQueue)
    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, jms)
    rapid.start()
}

fun rapidApp(rapid: RapidsConnection, kø: Kø) {
    rapid.apply {
        Utbetalinger(this, kø.sendSession())
        Kvitteringer(this, kø)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
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
