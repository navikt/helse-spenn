package no.nav.helse.spenn

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.spenn.config.*
import no.nav.helse.spenn.grensesnittavstemming.JAXBAvstemmingsdata
import no.nav.helse.spenn.grensesnittavstemming.SendTilAvstemmingTask
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.OppdragMQReceiver
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.overforing.SendToOSTask
import no.nav.helse.spenn.overforing.UtbetalingService
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.spennApiServer
import no.nav.helse.spenn.simulering.SendToSimuleringTask
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.KafkaStreamsConfig
import no.nav.helse.spenn.vedtak.fnr.AktorRegisteretClient
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.slf4j.LoggerFactory

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

private val log = LoggerFactory.getLogger("SpennServices")

@KtorExperimentalAPI
class SpennServices(appConfig: ApplicationConfig) : SpennTaskRunner {

    ////

    override fun sendToOS() = sendToOSTask.sendToOS()
    override fun sendSimulering() = sendToSimuleringTask.sendSimulering()
    override fun sendTilAvstemming() = sendTilAvstemmingTask.sendTilAvstemming()

    ////

    val metrics = PrometheusMeterRegistry(PrometheusConfig.DEFAULT) //Metrics.globalRegistry
    val spennConfig = SpennConfig.from(appConfig)

    ////// DATABASE ///////

    val spennDataSource = SpennDataSource.getMigratedDatasourceInstance(
            SpennDbConfig.from(appConfig))

    val oppdragStateService = OppdragStateService(
            OppdragStateJooqRepository(
                    spennDataSource.jooqDslContext
            )
    )

    ///// STS-REST /////

    val stsRestClient = StsRestClient(
            baseUrl = spennConfig.stsRestUrl,
            username = spennConfig.stsRestUsername,
            password = spennConfig.stsRestPassword
    )

    ///// AKTØR-reg /////

    val aktorTilFnrMapper = AktorRegisteretClient(
            stsRestClient = stsRestClient,
            aktorRegisteretUrl = spennConfig.aktorRegisteretBaseUrl
    )

    ///// KAFKA /////

    val kafkaStreamConsumer = KafkaStreamsConfig(
            oppdragStateService = oppdragStateService,
            meterRegistry = metrics,
            aktørTilFnrMapper = aktorTilFnrMapper,
            config = SpennKafkaConfig.from(appConfig))
            .streamConsumerStart()

    ////// MQ ///////

    val mqConfig = SpennMQConfig.from(appConfig)

    // TODO if (! mqConfig.mqEnabled) then what?
    val spennMQConnection =
            MQConnectionFactory().apply {
                hostName = mqConfig.hostname
                port = mqConfig.port
                channel = mqConfig.channel
                queueManager = mqConfig.queueManager
                transportType = WMQConstants.WMQ_CM_CLIENT
            }.createConnection(mqConfig.user, mqConfig.password)

    //// SIMULERING ////

    val simuleringConfig = SimuleringConfig(
            simuleringServiceUrl = spennConfig.simuleringServiceUrl,
            stsUrl = spennConfig.stsUrl,
            stsUsername = spennConfig.stsUsername,
            stsPassword = spennConfig.stsPassword
    )

    val simuleringService = SimuleringService(
            simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()),
            metrics
    )

    val sendToSimuleringTask = SendToSimuleringTask(
            simuleringService,
            oppdragStateService,
            metrics
    )

    ///// OPPDRAG ////

    val oppdragMQSender = OppdragMQSender(
            spennMQConnection,
            mqConfig.oppdragQueueSend,
            mqConfig.oppdragQueueMottak,
            JAXBOppdrag()
    )

    val oppdragMQReceiver = OppdragMQReceiver(
            spennMQConnection,
            mqConfig.oppdragQueueMottak,
            JAXBOppdrag(),
            oppdragStateService,
            metrics
    )

    val utbetalingService = UtbetalingService(oppdragMQSender)

    val sendToOSTask = SendToOSTask(
            oppdragStateService, utbetalingService, metrics
    )

    ///// AVSTEMMING /////

    val avstemmingMQSender = AvstemmingMQSender(
            spennMQConnection,
            mqConfig.avstemmingQueueSend,
            JAXBAvstemmingsdata()
    )

    val sendTilAvstemmingTask = SendTilAvstemmingTask(
            oppdragStateService, avstemmingMQSender, metrics
    )


    ///// HTTP API /////

    private val apiAuthConfig = SpennApiAuthConfig.from(appConfig)

    val spennApiServer = spennApiServer(SpennApiEnvironment(
            kafkaStreams = kafkaStreamConsumer.streams,
            meterRegistry = metrics,
            authConfig = apiAuthConfig,
            simuleringService = simuleringService,
            aktørTilFnrMapper = aktorTilFnrMapper,
            auditSupport = AuditSupport()
    ))

    ///// ///// /////

    fun shutdown() {
        log.info("Closing MQ Connection...")
        oppdragMQSender.close()
        oppdragMQReceiver.close()
        avstemmingMQSender.close()
        spennMQConnection.close()
        log.info("Closing MQ Connection done.")
        log.info("Closing datasource...")
        spennDataSource.dataSource.close()
        log.info("Closing datasource done.")
    }
}
