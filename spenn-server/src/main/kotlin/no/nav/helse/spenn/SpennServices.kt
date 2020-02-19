package no.nav.helse.spenn

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spenn.grensesnittavstemming.SendTilAvstemmingTask
import no.nav.helse.spenn.oppdrag.AvstemmingMQSender
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.overforing.OppdragMQSender
import no.nav.helse.spenn.overforing.SendToOSTask
import no.nav.helse.spenn.simulering.SendToSimuleringTask
import no.nav.helse.spenn.simulering.SimuleringService

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

class SpennServices(
    oppdragService: OppdragService,
    simuleringService: SimuleringService,
    oppdragMQSender: OppdragMQSender,
    avstemmingMQSender: AvstemmingMQSender
) : SpennTaskRunner {

    override fun sendToOS() = sendToOSTask.sendToOS()
    override fun sendSimulering() = sendToSimuleringTask.sendSimulering()
    override fun sendTilAvstemming() = sendTilAvstemmingTask.sendTilAvstemming()

    val sendToSimuleringTask = SendToSimuleringTask(
        simuleringService,
        oppdragService,
        metrics
    )

    val sendToOSTask = SendToOSTask(
        oppdragService, oppdragMQSender, metrics
    )

    val sendTilAvstemmingTask = SendTilAvstemmingTask(
        oppdragService, avstemmingMQSender, metrics
    )
}
