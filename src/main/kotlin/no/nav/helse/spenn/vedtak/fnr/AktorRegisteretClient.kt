package no.nav.helse.spenn.vedtak.fnr

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Component
@Profile(value=["preprod", "prod"])
class AktorRegisteretClient(val stsRestClient: StsRestClient,
                            @Value("\${AKTORREGISTERET_BASE_URL:http://aktoerregisteret}")val aktorRegisteretUrl: String) : AktørTilFnrMapper {


    override fun tilFnr(aktorId: String): Fodselsnummer {
        TODO()
    }

    fun lookUp(aktorId: String): JsonNode {
        val bearer = stsRestClient.token()
        val webClient = WebClient.builder().baseUrl(aktorRegisteretUrl).build()
        return webClient.get()
                .uri("/api/v1/identer?gjeldende=true")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $bearer")
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Consumer-Id", "spenn")
                .header("Nav-Personidenter", aktorId)
                .retrieve()
                .bodyToMono(JsonNode::class.java).block()!!

    }



}