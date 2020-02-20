package no.nav.helse.spenn.rest.api.v1

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagAnnulleringsoppdragFraBehov
import no.nav.helse.spenn.oppdrag.dao.AlleredeAnnulertException
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.toAnulleringsbehov
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("simuleringcontroller")

fun Route.opphørscontroller(
    oppdragService: OppdragService,
    audit: AuditSupport
) {
    post("/api/v1/opphor") {
        val behov: JsonNode
        try {
            behov = call.receive()
        } catch (jsonError: JsonProcessingException) {
            LOG.warn("JsonProcessingException: ", jsonError)
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val utbetalingsreferanse = behov["utbetalingsreferanse"].asText()!!
        val oppdrag = lagAnnulleringsoppdragFraBehov(behov.toAnulleringsbehov())
        LOG.info("opphør called for utbetalingsreferanse: $utbetalingsreferanse")
        audit.info("annulering kall for utbetalingsreferanse: $utbetalingsreferanse", call.authentication)
        try {
            oppdragService.annulerUtbetaling(oppdrag)
            call.respond(HttpStatusCode.Created, "Annuleringsoppdrag lagret")
        } catch (alleredeAnnulertException: AlleredeAnnulertException) {
            call.respond(HttpStatusCode.Conflict, "${alleredeAnnulertException.message}")
        }
    }
}
