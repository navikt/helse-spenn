package no.nav.helse.spenn

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.grensesnittavstemming.SendTilAvstemmingTask
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.OppdragMQReceiver
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.overforing.SendToOSTask
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.spennApiServer
import no.nav.helse.spenn.simulering.SendToSimuleringTask
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import org.apache.cxf.bus.extension.ExtensionManagerBus

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

class SpennServices(
    env: Environment,
    serviceUser: ServiceUser,
    oppdragService: OppdragService
) : SpennTaskRunner {

    override fun sendToOS() = sendToOSTask.sendToOS()
    override fun sendSimulering() = sendToSimuleringTask.sendSimulering()
    override fun sendTilAvstemming() = sendTilAvstemmingTask.sendTilAvstemming()

    ////// MQ ///////

    // TODO if (! mqConfig.mqEnabled) then what?
    val spennMQConnection =
        MQConnectionFactory().apply {
            hostName = env.mq.hostname
            port = env.mq.port
            channel = env.mq.channel
            queueManager = env.mq.queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
        }.createConnection(env.mq.user, env.mq.password)!!

    //// SIMULERING ////

    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.simuleringServiceUrl,
        stsSoapUrl = env.stsSoapUrl,
        serviceUser = serviceUser
    )

    val simuleringService = SimuleringService(
        simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()),
        metrics
    )

    val sendToSimuleringTask = SendToSimuleringTask(
        simuleringService,
        oppdragService,
        metrics
    )

    ///// OPPDRAG ////

    val oppdragMQSender = OppdragMQSender(
        spennMQConnection,
        env.mq.oppdragQueueSend,
        env.mq.oppdragQueueMottak,
        JAXBOppdrag()
    )

    val oppdragMQReceiver = OppdragMQReceiver(
        spennMQConnection,
        env.mq.oppdragQueueMottak,
        JAXBOppdrag(),
        oppdragService,
        metrics
    )

    val sendToOSTask = SendToOSTask(
        oppdragService, oppdragMQSender, metrics
    )

    ///// AVSTEMMING /////

    val avstemmingMQSender = AvstemmingMQSender(
        spennMQConnection,
        env.mq.avstemmingQueueSend,
        JAXBAvstemmingsdata()
    )

    val sendTilAvstemmingTask = SendTilAvstemmingTask(
        oppdragService, avstemmingMQSender, metrics
    )


    ///// HTTP API /////

    @KtorExperimentalAPI
    val spennApiServer = spennApiServer(
        SpennApiEnvironment(
            meterRegistry = metrics,
            authConfig = env.auth,
            simuleringService = simuleringService,
            auditSupport = AuditSupport(),
            stateService = oppdragService,
            oppdragMQSender = oppdragMQSender
        )
    )

    ///// ///// /////

    fun shutdown() {
        oppdragMQSender.close()
        oppdragMQReceiver.close()
        avstemmingMQSender.close()
        spennMQConnection.close()
    }
}
