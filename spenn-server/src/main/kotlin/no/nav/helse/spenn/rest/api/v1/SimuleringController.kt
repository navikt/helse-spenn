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
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.UtbetalingLøser.Companion.lagOppdragFraBehov
import no.nav.helse.spenn.oppdrag.dao.lagPåSidenSimuleringsrequest
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.toOppdragsbehov
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("simuleringcontroller")

@KtorExperimentalAPI
fun Route.simuleringcontroller(
    simuleringService: SimuleringService,
    audit: AuditSupport
) {

    post("/api/v1/simulering") {
        val behov: JsonNode
        try {
            behov = call.receive<JsonNode>()
        } catch (jsonError: JsonProcessingException) {
            LOG.warn("JsonProcessingException: ", jsonError)
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val sakskompleksId = behov["sakskompleksId"].asText()!!
        val aktørId = behov["aktørId"].asText()!!
        val erUtvidelse = behov["erUtvidelse"].asBoolean()
        val oppdrag = Utbetalingsbehov(behov, aktørTilFnrMapper.tilFnr(aktørId))
        val oppdrag = lagOppdragFraBehov(behov.toOppdragsbehov())
        LOG.info("simulering called for vedtak: $sakskompleksId")
        audit.info("simulering kall for vedtak: $sakskompleksId", call.authentication)
        call.respond(simuleringService.simulerOppdrag(
                oppdrag.lagPåSidenSimuleringsrequest(erUtvidelse = erUtvidelse)))
    }
}
