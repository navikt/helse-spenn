package no.nav.helse.spenn

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigResolveOptions
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.config.SpennConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledExecutorService


private val log = LoggerFactory.getLogger("SpennApplication")

@KtorExperimentalAPI
fun main() {
    log.info("Loading config...")
    val conf = ConfigFactory.parseResources("application.conf")
            .resolve(ConfigResolveOptions.defaults())
    val appConfig = HoconApplicationConfig(conf)
    spenn(appConfig)
}

@KtorExperimentalAPI
internal fun spenn(appConfig: ApplicationConfig) {
    log.info("Creating SpennServices...")
    log.info("OPPHØRSEKSPERIMENT!")
    val services = SpennServices(appConfig)

    val schedulerConfig = SpennConfig.from(appConfig)
    var scheduler: ScheduledExecutorService? = null
    if (schedulerConfig.schedulerEnabled) {
        log.info("Setting up schedules...")
        scheduler = setupSchedules(
                spennTasks = services,
                dataSourceForLockingTable = services.spennDataSource.dataSource,
                config = schedulerConfig)
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
