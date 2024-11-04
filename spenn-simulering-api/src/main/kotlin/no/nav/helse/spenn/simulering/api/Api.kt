package no.nav.helse.spenn.simulering.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

fun Route.api(simuleringtjeneste: Simuleringtjeneste) {
    post("/api/simulering") {
        val request = call.receiveNullable<SimuleringRequest>() ?: throw BadRequestException("Ugyldig simulering request, oppfyller ikke kontrakten")
        sikkerlogg.info("request body:\n${call.receiveText()}")
        when (val svar = simuleringtjeneste.simulerOppdrag(request)) {
            is SimuleringResponse.Ok -> call.respond(HttpStatusCode.OK, svar.simulering)
            SimuleringResponse.OkMenTomt -> call.respond(HttpStatusCode.NoContent)
            is SimuleringResponse.FunksjonellFeil -> throw BadRequestException("Simulering feilet på grunn av funksjonell feil. ${svar.feilmelding}")
            SimuleringResponse.OppdragsystemetErStengt -> call.respond(HttpStatusCode.ServiceUnavailable)
            is SimuleringResponse.TekniskFeil -> throw Exception(svar.feilmelding)
        }
    }
}
