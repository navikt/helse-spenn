package no.nav.helse.spenn.vedtak.fnr

import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Component
@Profile(value=["preprod", "prod"])
class FnrOppslag(val stsRestClient: StsRestClient,
                 @Value("\${SPARKEL_BASE_URL:http://sparkel}") val sparkelBaseUrl: String): AktørTilFnrMapper {

    override fun tilFnr(aktørId: String): Fodselsnummer {
        val bearer = stsRestClient.token()
        val webClient = WebClient.builder().baseUrl(sparkelBaseUrl).build()
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
@Profile(value=["test", "default", "integration"])
class DummyAktørMapper() : AktørTilFnrMapper {
    override fun tilFnr(aktørId: String): Fodselsnummer = aktørId
}


interface AktørTilFnrMapper {
    fun tilFnr(aktørId: String): Fodselsnummer
}

