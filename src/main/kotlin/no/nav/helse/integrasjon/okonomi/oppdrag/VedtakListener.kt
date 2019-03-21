package no.nav.helse.integrasjon.okonomi.oppdrag

import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topics
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.streamConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*


class VedtakListener(val env: Environment) {
    private val log = LoggerFactory.getLogger(VedtakListener::class.java)
    private val consumer: StreamConsumer
    private val appId: String = "spenn-1"
    init {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()

        builder.consumeTopic(Topics.VEDTAK_SYKEPENGER)
                .peek{_, _ -> log.info("here goes payments")}

        return builder.build()
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

}