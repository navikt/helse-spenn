package no.nav.helse.spenn

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigResolveOptions
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.micrometer.core.instrument.Metrics
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.helse.spenn.config.SpennMQConfig
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
import no.nav.helse.spenn.rest.SpennApiAuthConfig
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.spennApiServer
import no.nav.helse.spenn.simulering.SendToSimuleringTask
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.KafkaStreamsConfig
import no.nav.helse.spenn.vedtak.SpennKafkaConfig
import no.nav.helse.spenn.vedtak.fnr.AktorRegisteretClient
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


/*import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication*/

/*@EnableOIDCTokenValidation
@SpringBootApplication(scanBasePackages = ["no.nav.helse"])*/
//class Application
//fun main(args: Array<String>) {
/*runApplication<Application>(*args) {
    setBannerMode(Banner.Mode.OFF)
}*/
//}

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


private val log = LoggerFactory.getLogger("SpennApplication")


fun main(args: Array<String>) {
    log.info("Application.module starting...")

    val conf = ConfigFactory.parseResources("application.conf")
            .resolve(ConfigResolveOptions.defaults())
    val appConfig = HoconApplicationConfig(conf)

    val services = SpennServices(appConfig)

    val schedulerConfig = SpennConfig.from(appConfig)
    var scheduler: ScheduledExecutorService? = null
    if (schedulerConfig.schedulerEnabled) {
        scheduler = setupSchedules(services, schedulerConfig)
    }

    log.info("Starting HTTP API services")
    services.spennApiServer.start()

    log.info("Starting MQConnection to start cunsuming replies on MQQueue...")
    services.spennMQConnection.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutting down scheduler...")
        scheduler?.shutdown()
        log.info("Shutting down scheduler done.")

        log.info("Shutting down services...")
        services.shutdown()
        log.info("Shutting down services done.")
    })
}


private fun setupSchedules(services: SpennServices, config: SpennConfig): ScheduledExecutorService {
    log.info("setting up scheduler")
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val lockProvider = JdbcLockProvider(services.spennDataSource.dataSource, "shedlock")
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
    val defaultMaxWaitForLockInSeconds = 10L

    val wrapWithErrorLogging = fun(fn: () -> Unit) {
        try {
            fn()
        } catch (e: Exception) {
            log.error("Error running scheduled task", e)
        }
    }

    if (config.taskOppdragEnabled) {
        log.info("Scheduling sendToOSTask")
        scheduler.scheduleAtFixedRate({
            lockingExecutor.executeWithLock(Runnable {
                wrapWithErrorLogging {
                    services.sendToOSTask.sendToOS()
                }
            }, LockConfiguration(
                    "sendToOS",
                    Instant.now().plusSeconds(defaultMaxWaitForLockInSeconds)))
        }, 5, 60, TimeUnit.SECONDS)
    }

    if (config.taskSimuleringEnabled) {
        log.info("Scheduling sendToSimuleringTask")
        scheduler.scheduleAtFixedRate({
            log.info("sendToSimuleringTask - before lock")
            // TODO: Ikke kjør mellom kl 21 og 07
            lockingExecutor.executeWithLock(Runnable {
                wrapWithErrorLogging {
                    services.sendToSimuleringTask.sendSimulering()
                }
            }, LockConfiguration(
                    "sendToSimulering",
                    Instant.now().plusSeconds(defaultMaxWaitForLockInSeconds)))
        }, 10, 30, TimeUnit.SECONDS)
    }

    if (config.taskAvstemmingEnabled) {
        log.info("Scheduling sendTilAvstemmingTask")
        scheduler.scheduleAtFixedRate({
            lockingExecutor.executeWithLock(Runnable {
                wrapWithErrorLogging {
                    services.sendTilAvstemmingTask.sendTilAvstemming()
                }
            }, LockConfiguration(
                    "sendTilAvstemming",
                    Instant.now().plusSeconds(defaultMaxWaitForLockInSeconds)))
        }, 12, 24, TimeUnit.HOURS) // TODO: bør gå fast klokkeslett
    }

    return scheduler
}


class SpennServices(appConfig: ApplicationConfig) {

    val metrics = Metrics.globalRegistry
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
            simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus())
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
            auditSupport = AuditSupport(apiAuthConfig)
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
