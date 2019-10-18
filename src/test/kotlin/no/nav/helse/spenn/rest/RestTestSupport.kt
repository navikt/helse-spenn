package no.nav.helse.spenn.rest

import org.apache.kafka.streams.KafkaStreams
import org.mockito.Mockito

internal fun SpennApiEnvironment.mockKafkaIsRunning(isRunning: Boolean = true) {
    Mockito.reset(this.kafkaStreams)
    val mockState = Mockito.mock(KafkaStreams.State::class.java)
    Mockito.`when`(mockState.isRunning).thenReturn(isRunning)
    Mockito.`when`(this.kafkaStreams.state()).thenReturn(mockState)
}