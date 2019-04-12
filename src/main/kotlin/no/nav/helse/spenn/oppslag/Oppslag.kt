package no.nav.helse.spenn.oppslag

import no.nav.helse.Environment
import no.nav.helse.spenn.vedtak.AktørTilFnrMapper
import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Component
class FnrOppslag(val env: Environment, val stsRestClient: StsRestClient): AktørTilFnrMapper {
    private val log = LoggerFactory.getLogger(FnrOppslag::class.java.name)
    override fun tilFnr(aktørId: String): Fodselsnummer {
        val bearer = stsRestClient.token()
        val webClient = WebClient.builder().baseUrl(env.sparkelBaseUrl).build()
        return webClient.get()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $bearer")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Consumer-Id", "spenn")
                .retrieve()
                .bodyToMono(Fodselsnummer::class.java).block()!!
    }

}