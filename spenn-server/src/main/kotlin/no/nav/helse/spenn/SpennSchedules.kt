package no.nav.helse.spenn

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
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

internal fun setupSchedules(
    spennTasks: SpennTaskRunner,
    dataSourceForLockingTable: DataSource,
    clock: Clock = Clock.systemDefaultZone()
): ScheduledExecutorService {

    log.info("setting up scheduler")
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val lockProvider = JdbcLockProvider(dataSourceForLockingTable, "shedlock")
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
    val defaultMaxWaitForLockInSeconds = 10L

    fun runWithLock(lockName: String, fn: () -> Unit) {
        lockingExecutor.executeWithLock(
            Runnable {
                try {
                    fn()
                } catch (e: Exception) {
                    log.error("Error running scheduled task", e)
                }
            }, LockConfiguration(
                lockName,
                Instant.now().plusSeconds(defaultMaxWaitForLockInSeconds)
            )
        )
    }

    log.info("Scheduling sendToOSTask")
    scheduler.scheduleAtFixedRate({
        runWithLock("sendToOS") {
            spennTasks.sendToOS()
        }
    }, 500, 500, TimeUnit.MILLISECONDS)

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
    }, 500, 500, TimeUnit.MILLISECONDS)

    val avstemmingsTidspunktTime = 22
    val avstemmingsTidspunktMinutt = 0

    val now = LocalDateTime.now(clock)
    var nextRun = now.withHour(avstemmingsTidspunktTime).withMinute(avstemmingsTidspunktMinutt).withSecond(0)
    if (now > nextRun) {
        nextRun = nextRun.plusDays(1)
    }

    log.info("Scheduling sendTilAvstemmingTask (first will be at $nextRun)")

    val initialDelay = Duration.between(now, nextRun).seconds

    scheduler.scheduleAtFixedRate({
        runWithLock("sendTilAvstemming") {
            spennTasks.sendTilAvstemming()
        }
    }, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS)

    return scheduler
}
