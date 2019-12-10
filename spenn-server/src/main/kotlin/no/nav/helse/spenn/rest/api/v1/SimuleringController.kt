package no.nav.helse.spenn.rest.api.v1

import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spenn.oppdrag.dao.lagPåSidenSimuleringsrequest
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.Utbetalingsbehov
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("simuleringcontroller")

@KtorExperimentalAPI
fun Route.simuleringcontroller(
    simuleringService: SimuleringService,
    aktørTilFnrMapper: AktørTilFnrMapper,
    audit: AuditSupport
) {

    post("/api/v1/simulering") {
        val behov: Utbetalingsbehov
        try {
            behov = call.receive()
        } catch (jsonError: JsonProcessingException) {
            LOG.warn("JsonProcessingException: ", jsonError)
            call.respond(HttpStatusCode.BadRequest);
            return@post
        }
        LOG.info("simulering called for vedtak: ${behov.sakskompleksId}")
        audit.info("simulering kall for vedtak: ${behov.sakskompleksId}", call.authentication)
        call.respond(simuleringService.simulerOppdrag(
            behov.tilUtbetaling(aktørTilFnrMapper.tilFnr(behov.aktørId))
                .lagPåSidenSimuleringsrequest()))
    }
}