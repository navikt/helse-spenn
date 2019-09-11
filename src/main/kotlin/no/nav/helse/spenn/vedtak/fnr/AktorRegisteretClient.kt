package no.nav.helse.spenn.vedtak.fnr

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.lang.RuntimeException
import java.util.*

@Component
@Profile(value=["preprod", "prod"])
class AktorRegisteretClient(val stsRestClient: StsRestClient,
                            @Value("\${AKTORREGISTERET_BASE_URL:http://aktoerregisteret}")val aktorRegisteretUrl: String) : AktørTilFnrMapper {

    companion object {
        private val LOG = LoggerFactory.getLogger(AktorRegisteretClient::class.java)
    }

    override fun tilFnr(aktorId: String): Fodselsnummer {
        val node = lookUp(aktorId).elements().next()
        val identer = node["identer"]
        if (identer.isNull) {
            LOG.error("Could not lookup aktorId: ${aktorId}")
            throw AktorRegisterException(node["feilmelding"].asText())
        }
        return identer.filter {
            it["identgruppe"].textValue() == "NorskIdent"
        }.first()["ident"].asText()
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

class AktorRegisterException(message: String?) : Exception(message)
