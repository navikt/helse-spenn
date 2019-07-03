package no.nav.helse.spenn.oppslag

import no.nav.helse.spenn.Environment
import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Component
@Profile(value=["preprod", "prod"])
class FnrOppslag(val env: Environment, val stsRestClient: StsRestClient): AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer {
        val bearer = stsRestClient.token()
        val webClient = WebClient.builder().baseUrl(env.sparkelBaseUrl).build()
        return webClient.get()
                .uri("/api/aktor/$aktørId/fnr")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $bearer")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Consumer-Id", "spenn")
                .retrieve()
                .bodyToMono(Fodselsnummer::class.java).block()!!
    }

}

@Component
@Profile(value=["test", "default"])
class DummyAktørMapper() : AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer = aktørId
}


interface AktørTilFnrMapper {
    fun tilFnr(aktørId: String): Fodselsnummer
}

