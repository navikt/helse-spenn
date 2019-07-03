package no.nav.helse.spenn.vedtak

import org.apache.kafka.streams.KafkaStreams

import org.slf4j.LoggerFactory



class StreamConsumer(val consumerName: String,
                     val streams: KafkaStreams) {

    companion object {
        private val log = LoggerFactory.getLogger(StreamConsumer::class.java)
    }

    fun start() {
        addShutdownHook()
        streams.start()
        log.info("Started stream consumer $consumerName")
    }

    fun stop(){
        streams.close()
    }


    fun state(): KafkaStreams.State? {
        return streams.state()
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

