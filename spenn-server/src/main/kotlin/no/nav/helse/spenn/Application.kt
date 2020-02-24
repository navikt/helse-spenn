package no.nav.helse.spenn

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.grensesnittavstemming.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.OppdragMQReceiver
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.spennApiModule
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import org.apache.cxf.bus.extension.ExtensionManagerBus
import javax.jms.Connection


val metrics = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

@KtorExperimentalAPI
fun main() {
    val env: Environment = readEnvironment()
    val serviceUser: ServiceUser = readServiceUserCredentials()
    setUpAndLaunchApplication(env, serviceUser)
}

@KtorExperimentalAPI
fun setUpAndLaunchApplication(env: Environment, serviceUser: ServiceUser) {
    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.simuleringServiceUrl,
        stsSoapUrl = env.stsSoapUrl,
        serviceUser = serviceUser,
        disableCNCheck = true
    )

    val simuleringService = SimuleringService(simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()))

    val mqConnection =
        MQConnectionFactory().apply {
            hostName = env.mqConnection.hostname
            port = env.mqConnection.port
            channel = env.mqConnection.channel
            queueManager = env.mqConnection.queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
        }.createConnection(env.mqConnection.user, env.mqConnection.password)!!

    val dataSource = SpennDataSource.getMigratedDatasourceInstance(env.db).dataSource

    val oppdragService = OppdragService(dataSource)

    val rapidsConnection = RapidApplication.Builder(env.raw).withKtorModule {
        spennApiModule(
            SpennApiEnvironment(
                meterRegistry = metrics,
                authConfig = env.auth,
                simuleringService = simuleringService,
                auditSupport = AuditSupport(),
                stateService = oppdragService
            )
        )
    }.build()

    launchApplication(simuleringService, mqConnection, dataSource, oppdragService, rapidsConnection, env.mqQueues)
}

@KtorExperimentalAPI
internal fun launchApplication(
    simuleringService: SimuleringService,
    mqConnection: Connection,
    dataSource: HikariDataSource,
    oppdragService: OppdragService,
    rapidsConnection: RapidsConnection,
    mqQueues: MqQueuesEnvironment
) {
    val oppdragMQSender = OppdragMQSender(
        mqConnection,
        mqQueues.oppdragQueueSend,
        mqQueues.oppdragQueueMottak
    )

    val oppdragMQReceiver = OppdragMQReceiver(
        mqConnection,
        mqQueues.oppdragQueueMottak,
        rapidsConnection,
        oppdragService,
        metrics
    )

    val avstemmingMQSender = AvstemmingMQSender(
        mqConnection,
        mqQueues.avstemmingQueueSend,
        JAXBAvstemmingsdata()
    )

    val services = SpennServices(
        oppdragService = oppdragService,
        simuleringService = simuleringService,
        oppdragMQSender = oppdragMQSender,
        avstemmingMQSender = avstemmingMQSender
    )

    UtbetalingLøser(rapidsConnection, oppdragService)

    val scheduler = setupSchedules(
        spennTasks = services,
        dataSourceForLockingTable = dataSource
    )

    mqConnection.start()
    rapidsConnection.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        rapidsConnection.stop()
        dataSource.close()

        mqConnection.close()
        scheduler.shutdown()
    })
}
