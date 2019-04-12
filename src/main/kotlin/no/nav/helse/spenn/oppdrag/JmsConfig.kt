package no.nav.helse.spenn.oppdrag

import com.ibm.mq.constants.MQConstants
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.helse.Environment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter

import javax.jms.ConnectionFactory;

@Configuration
@Profile("prod")
class JmsConfig(val env: Environment) {

	private final val UTF_8_WITH_PUA = 1208

	@Bean
	fun wmqConnectionFactory():ConnectionFactory {
        val connectionFactory = MQConnectionFactory().apply {
            hostName = env.mqHostname
            port = env.mqPort!!.toInt()
            channel = env.mqChannel
            queueManager = env.queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
            ccsid = UTF_8_WITH_PUA
            setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQConstants.MQENC_NATIVE)
            setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
        }
        return UserCredentialsConnectionFactoryAdapter().apply {
            setTargetConnectionFactory(connectionFactory)
            setUsername(env.mqUsername!!)
            setPassword(env.mqPassword!!)
        }

	}

}