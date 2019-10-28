package no.nav.helse.spenn

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.helse.spenn.config.SpennConfig
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("SpennSchedules")

internal fun setupSchedules(services: SpennServices, config: SpennConfig): ScheduledExecutorService {
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
