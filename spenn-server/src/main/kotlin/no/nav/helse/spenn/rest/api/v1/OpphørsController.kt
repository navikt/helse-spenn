package no.nav.helse.spenn.rest.api.v1

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.vedtak.SpennOppdragFactory
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("simuleringcontroller")

@KtorExperimentalAPI
fun Route.opphørscontroller(
    oppdragService: OppdragService,
    aktørTilFnrMapper: AktørTilFnrMapper,
    audit: AuditSupport
) {
    post("/api/v1/opphor") {
        val behov: JsonNode
        try {
            behov = call.receive()
        } catch (jsonError: JsonProcessingException) {
            LOG.warn("JsonProcessingException: ", jsonError)
            call.respond(HttpStatusCode.BadRequest);
            return@post
        }
        val utbetalingsreferanse = behov["utbetalingsreferanse"].asText()!!
        val aktørId = behov["aktørId"].asText()!!
        val oppdrag = SpennOppdragFactory.lagOppdragFraBehov(behov, aktørTilFnrMapper.tilFnr(aktørId))
        LOG.info("opphør called for utbetalingsreferanse: $utbetalingsreferanse")
        //audit.info("annulering kall for utbetalingsreferanse: $utbetalingsreferanse", call.authentication)
        oppdragService.annulerUtbetaling(oppdrag)
        call.respond(HttpStatusCode.Created, "Annuleringsoppdrag lagret")
    }
}