package no.nav.helse.spenn.simulering.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

fun Route.api(simuleringtjeneste: Simuleringtjeneste) {
    post("/api/simulering") {
        val request = call.receiveNullable<SimuleringRequest>() ?: return@post call.respond(HttpStatusCode.BadRequest, FeilResponse(
            feilmelding = "Ugyldig request",
            callId = call.callId
        ))
        sikkerlogg.info("request body:\n${call.receiveText()}")
        val callId = call.callId ?: throw BadRequestException("Mangler callId-header")
        when (val svar = simuleringtjeneste.simulerOppdrag(request)) {
            is SimuleringResponse.Ok -> call.respond(HttpStatusCode.OK, svar.simulering)
            is SimuleringResponse.FunksjonellFeil -> call.respond(HttpStatusCode.BadRequest, FeilResponse(
                feilmelding = "Simulering feilet på grunn av funksjonell feil. ${svar.feilmelding}",
                callId = callId
            ))
            SimuleringResponse.OppdragsystemetErStengt -> call.respond(HttpStatusCode.ServiceUnavailable)
            is SimuleringResponse.TekniskFeil -> throw Exception(svar.feilmelding)
        }
    }
}
