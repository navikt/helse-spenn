package no.nav.helse.spenn

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.JAXBAvstemmingsdata
import no.nav.helse.spenn.oppdrag.JAXBOppdrag
import no.nav.helse.spenn.oppdrag.OppdragMQReceiver
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.rest.SpennApiEnvironment
import no.nav.helse.spenn.rest.api.v1.AuditSupport
import no.nav.helse.spenn.rest.spennApiModule
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import org.apache.cxf.bus.extension.ExtensionManagerBus
import java.util.concurrent.ScheduledExecutorService
import javax.jms.Connection
import javax.sql.DataSource


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
        serviceUser = serviceUser
    )

    val simuleringService = SimuleringService(
        simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()),
        metrics
    )

    val mqConnection =
        MQConnectionFactory().apply {
            hostName = env.mqConnection.hostname
            port = env.mqConnection.port
            channel = env.mqConnection.channel
            queueManager = env.mqConnection.queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
        }.createConnection(env.mqConnection.user, env.mqConnection.password)!!

    val dataSource = SpennDataSource.getMigratedDatasourceInstance(env.db).dataSource

    launchApplication(simuleringService, mqConnection, dataSource, env.mqQueues, env.auth, env.raw)
}

@KtorExperimentalAPI
internal fun launchApplication(
    simuleringService: SimuleringService,
    mqConnection: Connection,
    dataSource: DataSource,
    mqQueues: MqQueuesEnvironment,
    auth: AuthEnvironment,
    rawEnvironment: Map<String, String>
) {
    val oppdragService = OppdragService(dataSource)

    val oppdragMQSender = OppdragMQSender(
        mqConnection,
        mqQueues.oppdragQueueSend,
        mqQueues.oppdragQueueMottak,
        JAXBOppdrag()
    )

    val oppdragMQReceiver = OppdragMQReceiver(
        mqConnection,
        mqQueues.oppdragQueueMottak,
        JAXBOppdrag(),
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

    val rapidsConnection = RapidApplication.Builder(rawEnvironment).withKtorModule {
        spennApiModule(
            SpennApiEnvironment(
                meterRegistry = metrics,
                authConfig = auth,
                simuleringService = simuleringService,
                auditSupport = AuditSupport(),
                stateService = oppdragService
            )
        )
    }.build()

    UtbetalingLøser(rapidsConnection, oppdragService)

    val scheduler: ScheduledExecutorService?
    scheduler = setupSchedules(
        spennTasks = services,
        dataSourceForLockingTable = dataSource
    )

    mqConnection.start()
    rapidsConnection.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        rapidsConnection.stop()
        dataSource.connection.close()
        mqConnection.stop()
        scheduler.shutdown()
    })
}
