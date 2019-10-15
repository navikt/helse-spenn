package no.nav.helse.spenn.oppdrag

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
/*import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import org.springframework.jms.core.JmsTemplate*/
import javax.jms.ConnectionFactory

/*@Configuration
@ConditionalOnProperty(name= ["jms.mq.enabled"], havingValue = "true")
@Profile(value = ["preprod"])*/
// Only works with MQ that we are runing in preprod and prod.
class JmsConfig(//@Value("\${MQ_HOSTNAME}")
                val mqHostname: String,
        //@Value("\${MQ_CHANNEL}")
                val mqChannel: String,
        //@Value("\${MQ_PORT}")
                val mqPort: String,
        //@Value("\${MQ_QUEUE_MANAGER}")
                val queueManager: String,
        //@Value("\${MQ_USERNAME}")
                val mqUsername: String,
        //@Value("\${MQ_PASSWORD}")
                val mqPassword: String) {

    //@Bean
    fun wmqConnectionFactory():ConnectionFactory {
        val connectionFactory = MQConnectionFactory().apply {
            hostName = mqHostname
            port = mqPort.toInt()
            channel = mqChannel
            queueManager = queueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
        }
        /*return UserCredentialsConnectionFactoryAdapter().apply {
            setTargetConnectionFactory(connectionFactory)
            setUsername(mqUsername)
            setPassword(mqPassword)
        }*/
        return connectionFactory
    }

    /*
    @Bean
    fun jmsTemplate(connectionFactory: ConnectionFactory): JmsTemplate {
        return JmsTemplate(connectionFactory)
    }*/

}