package no.nav.helse.spenn

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.micrometer.core.instrument.Metrics
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.dao.OppdragStateJooqRepository
import no.nav.helse.spenn.oppdrag.dao.OppdragStateService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.overforing.SendToOSTask
import no.nav.helse.spenn.overforing.UtbetalingService
import no.nav.helse.spenn.simulering.SendToSimuleringTask
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import org.apache.cxf.bus.extension.ExtensionManagerBus
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.sql.DataSource
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import no.nav.helse.spenn.grensesnittavstemming.JAXBAvstemmingsdata
import no.nav.helse.spenn.grensesnittavstemming.SendTilAvstemmingTask
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.vedtak.KafkaStreamsConfig
import no.nav.helse.spenn.vedtak.SpennKafkaConfig
import no.nav.helse.spenn.vedtak.fnr.DummyAktørMapper
import java.time.Instant


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

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


private val log = LoggerFactory.getLogger("SpennApplication")


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
@io.ktor.util.KtorExperimentalAPI
fun Application.module(testing: Boolean = false) {
    log.info("Application.module starting...")

    val appConfig = this.environment.config

    val services = SpennServices(appConfig)

    val schedulerConfig = SpennConfig.from(appConfig)
    var scheduler:ScheduledExecutorService? = null
    if (schedulerConfig.schedulerEnabled) {
        scheduler = setupSchedules(services, schedulerConfig)
    }

    //log.info("Starting kafka stream")
    //services.kafkaStreamConsumer.start()
    println("STATE IS")
    println(services.kafkaStreamConsumer.state())


    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutting down scheduler...")
        scheduler?.shutdown()
        log.info("Shutting down scheduler done.")

        log.info("Shutting down services...")
        services.shutdown()
        log.info("Shutting down services done.")
    })
}



private fun setupSchedules(services: SpennServices, config: SpennConfig) : ScheduledExecutorService {
    log.info("setting up scheduler")
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val lockProvider = JdbcLockProvider(services.dataSource, "shedlock")
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
    val defaultMaxWaitForLockInSeconds = 10L

    val wrapWithErrorLogging = fun(fn : () -> Unit ) {
        try {
            fn()
        } catch (e : Exception) {
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

    val dataSource = createMigratedTestDatasource() // TODO: FIX
    val jooqDSLContext = DSL.using(dataSource, SQLDialect.H2) // TODO: FIX

    val oppdragStateService = OppdragStateService(
            OppdragStateJooqRepository(
                    jooqDSLContext
            )
    )

    ///// KAFKA /////

    val kafkaStreamConsumer = KafkaStreamsConfig(
            oppdragStateService = oppdragStateService,
            meterRegistry = metrics,
            aktørTilFnrMapper = DummyAktørMapper(), // TODO
            config = SpennKafkaConfig.from(appConfig))
            .streamConsumerStart()

    ////// MQ ///////

    val spennMQConnection = MQConnectionFactory().apply {
        hostName = "localhost"
        port = 1414
        channel = "DEV.ADMIN.SVRCONN"
        queueManager = "QM1"
        transportType = WMQConstants.WMQ_CM_CLIENT
    }.createConnection("admin", "passw0rd")

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
            "DEV.QUEUE.1",
            "DEV.QUEUE.3",
            JAXBOppdrag()
    )

    val utbetalingService = UtbetalingService(oppdragMQSender)

    val sendToOSTask = SendToOSTask(
            oppdragStateService, utbetalingService, metrics
    )

    ///// AVSTEMMING /////

    val avstemmingMQSender = AvstemmingMQSender(
            spennMQConnection,
            "DEV.QUEUE.2",
            JAXBAvstemmingsdata()
    )

    val sendTilAvstemmingTask = SendTilAvstemmingTask(
            oppdragStateService, avstemmingMQSender, metrics
    )

    ///// ///// /////

    fun shutdown() {
        log.info("Closing MQ Connection...")
        oppdragMQSender.close()
        spennMQConnection.close()
        log.info("Closing MQ Connection done.")
        log.info("Closing datasource...")
        dataSource.close()
        log.info("Closing datasource done.")
    }
}


//// TO FIX:

fun createMigratedTestDatasource() : HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        maximumPoolSize = 5
        minimumIdle=1
    }
    val flyDS = HikariDataSource(hikariConfig)
    Flyway.configure().dataSource(flyDS)
            .load()
            .migrate()
    return flyDS
}



