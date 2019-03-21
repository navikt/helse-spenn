package no.nav.helse.spenn

import io.prometheus.client.hotspot.DefaultExports

import org.apache.kafka.streams.KafkaStreams

import org.slf4j.LoggerFactory



class StreamConsumer(val consumerName: String,
                     val streams: KafkaStreams) {

    private val log = LoggerFactory.getLogger(consumerName)

    fun start() {
        addShutdownHook()

        DefaultExports.initialize()
        streams.start()
        log.info("Started stream consumer $consumerName")
    }

    fun stop(){
        streams.close()
    }



    private fun addShutdownHook() {
        streams.setStateListener { newState, oldState ->
            log.info("From state={} to state={}", oldState, newState)

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                log.warn("No reason to keep living, closing stream")
                stop()
            }
        }
        streams.setUncaughtExceptionHandler{ _, ex ->
            log.error("Caught exception in stream, exiting", ex)
            stop()
        }
        Thread.currentThread().setUncaughtExceptionHandler { _, ex ->
            log.error("Caught exception, exiting", ex)
            stop()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

}

