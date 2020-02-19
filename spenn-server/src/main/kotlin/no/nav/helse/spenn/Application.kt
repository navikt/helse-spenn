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

    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.simuleringServiceUrl,
        stsSoapUrl = env.stsSoapUrl,
        serviceUser = serviceUser
    )

    val simuleringService = SimuleringService(
        simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()),
        metrics
    )

    val spennMQConnection =
        MQConnectionFactory().apply {
            hostName = env.mq.hostname
            port = env.mq.port
            channel = env.mq.channel
            queueManager = env.mq.queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
        }.createConnection(env.mq.user, env.mq.password)!!

    val oppdragMQSender = OppdragMQSender(
        spennMQConnection,
        env.mq.oppdragQueueSend,
        env.mq.oppdragQueueMottak,
        JAXBOppdrag()
    )

    val oppdragMQReceiver = OppdragMQReceiver(
        spennMQConnection,
        env.mq.oppdragQueueMottak,
        JAXBOppdrag(),
        oppdragService,
        metrics
    )

    val avstemmingMQSender = AvstemmingMQSender(
        spennMQConnection,
        env.mq.avstemmingQueueSend,
        JAXBAvstemmingsdata()
    )

    val services = SpennServices(
        oppdragService = oppdragService,
        simuleringService = simuleringService,
        oppdragMQSender = oppdragMQSender,
        avstemmingMQSender = avstemmingMQSender
    )

    val rapidsConnection = RapidApplication.Builder(env.raw).withKtorModule {
        spennApiModule(
            SpennApiEnvironment(
                meterRegistry = metrics,
                authConfig = env.auth,
                simuleringService = simuleringService,
                auditSupport = AuditSupport(),
                stateService = oppdragService,
                oppdragMQSender = oppdragMQSender
            )
        )
    }.build()

    UtbetalingLøser(rapidsConnection, oppdragService)

    val scheduler: ScheduledExecutorService?
    scheduler = setupSchedules(
        spennTasks = services,
        dataSourceForLockingTable = spennDataSource.dataSource
    )

    spennMQConnection.start()
    rapidsConnection.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        rapidsConnection.stop()
        spennDataSource.dataSource.close()
        spennMQConnection.stop()
        scheduler.shutdown()
    })
}
