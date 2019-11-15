package no.nav.helse.spenn

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.helse.spenn.config.SpennConfig
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("SpennSchedules")

interface SpennTaskRunner {
    fun sendToOS()
    fun sendSimulering()
    fun sendTilAvstemming()
}

internal fun setupSchedules(spennTasks: SpennTaskRunner,
                            dataSourceForLockingTable: DataSource,
                            config: SpennConfig,
                            clock: Clock = Clock.systemDefaultZone()): ScheduledExecutorService {
    
    log.info("setting up scheduler")
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val lockProvider = JdbcLockProvider(dataSourceForLockingTable, "shedlock")
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
    val defaultMaxWaitForLockInSeconds = 10L

    val runWithLock = fun(lockName: String, fn: () -> Unit) {
        lockingExecutor.executeWithLock(Runnable {
            try {
                fn()
            } catch (e: Exception) {
                log.error("Error running scheduled task", e)
            }
        }, LockConfiguration(
                lockName,
                Instant.now().plusSeconds(defaultMaxWaitForLockInSeconds)))
    }

    if (config.taskOppdragEnabled) {
        log.info("Scheduling sendToOSTask")
        scheduler.scheduleAtFixedRate({
            runWithLock("sendToOS") {
                spennTasks.sendToOS()
            }
        }, 45, 60, TimeUnit.SECONDS)
    } else {
        log.info("sendToOSTask is disabled")
    }

    if (config.taskSimuleringEnabled) {
        log.info("Scheduling sendToSimuleringTask")
        scheduler.scheduleAtFixedRate({
            val now = LocalDateTime.now(clock)
            if (now.hour < 7 || now.hour > 20) {
                log.info("Skipping sendToSimuleringTask between 21-7")
            } else {
                runWithLock("sendToSimulering") {
                    spennTasks.sendSimulering()
                }
            }
        }, 30, 30, TimeUnit.SECONDS)
    } else {
        log.info("sendToSimuleringTask is disabled")
    }

    if (config.taskAvstemmingEnabled) {
        val avstemmingsTidspunktTime = 14
        val avstemmingsTidspunktMinutt = 20

        val now = LocalDateTime.now(clock)
        var nextRun = now.withHour(avstemmingsTidspunktTime).withMinute(avstemmingsTidspunktMinutt).withSecond(0)
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1)
        }

        log.info("Scheduling sendTilAvstemmingTask (first will be at ${nextRun})")

        val initialDelay = Duration.between(now, nextRun).seconds

        scheduler.scheduleAtFixedRate({
            runWithLock("sendTilAvstemming") {
                spennTasks.sendTilAvstemming()
            }
        }, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS)
    } else {
        log.info("avstemming is disabled")
    }

    return scheduler
}
