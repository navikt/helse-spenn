package no.nav.helse.spenn.rest.api.v1

import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.helse.spenn.vedtak.tilUtbetaling
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("simuleringcontroller")

fun Route.simuleringcontroller(simuleringService: SimuleringService,
                               aktørTilFnrMapper: AktørTilFnrMapper,
                               audit: AuditSupport) {

    post("/api/v1/simulering") {
        val vedtak:Vedtak
        try {
            vedtak = call.receive()
        } catch (jsonError: JsonProcessingException) {
            LOG.info("JsonProcessingException")
            LOG.debug("JsonProcessingException: ", jsonError)
            call.respond(HttpStatusCode.BadRequest);
                    //"Feil her: ${jsonError.message}")
            return@post
        }
        LOG.info("simulering called for vedtak: ${vedtak.soknadId}")
        audit.info("simulering kall for vedtak: ${vedtak.soknadId}", call.authentication)
        val oppdrag = OppdragStateDTO(soknadId = vedtak.soknadId,
                utbetalingsOppdrag = vedtak.tilUtbetaling(aktørTilFnrMapper.tilFnr(vedtak.aktorId)))
        call.respond(simuleringService.runSimulering(oppdrag).simuleringResult!!)
    }
}