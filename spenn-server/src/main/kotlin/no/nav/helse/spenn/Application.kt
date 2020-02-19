package no.nav.helse.spenn

import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import java.util.concurrent.ScheduledExecutorService


val metrics = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

@KtorExperimentalAPI
fun main() {
    val env: Environment = readEnvironment()
    val serviceUser: ServiceUser = readServiceUserCredentials()

    launchApplication(env, serviceUser)
}

@KtorExperimentalAPI
internal fun launchApplication(env: Environment, serviceUser: ServiceUser) {
    val spennDataSource = SpennDataSource.getMigratedDatasourceInstance(env.db)
    val oppdragService = OppdragService(spennDataSource.dataSource)
    val rapidsConnection = rapidsConnection(env, oppdragService)


    val services = SpennServices(env, serviceUser, oppdragService)
    val scheduler: ScheduledExecutorService?

    scheduler = setupSchedules(
        spennTasks = services,
        dataSourceForLockingTable = spennDataSource.dataSource
    )

    services.spennApiServer.start()
    services.spennMQConnection.start()
    rapidsConnection.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        rapidsConnection.stop()
        spennDataSource.dataSource.close()
        scheduler.shutdown()
        services.shutdown()
    })
}

@KtorExperimentalAPI
internal fun rapidsConnection(env: Environment, oppdragService: OppdragService): RapidsConnection {
    val rapidsConnection = RapidApplication.create(env.raw)
    UtbetalingLøser(rapidsConnection, oppdragService)
    return rapidsConnection
}
