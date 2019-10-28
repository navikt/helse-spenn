package no.nav.helse.spenn.vedtak.fnr

import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class AktorRegisteretClient(val stsRestClient: StsRestClient,
        /*@Value("\${AKTORREGISTERET_BASE_URL}")*/ val aktorRegisteretUrl: String) : AktørTilFnrMapper {

    companion object {
        private val LOG = LoggerFactory.getLogger(AktorRegisteretClient::class.java)
    }

    override fun tilFnr(aktorId: String): Fodselsnummer {
        val identResponse = lookUp(aktorId).getJSONObject(aktorId)
        if (identResponse.isNull("identer")) {
            LOG.error("Could not lookup aktorId: ${aktorId}")
            throw AktorNotFoundException(identResponse.getString("feilmelding"), aktorId)
        }
        return identResponse.getJSONArray("identer")
                .map { it as JSONObject }
                .filter {
                    it.getString("identgruppe") == "NorskIdent"
                }.first().getString("ident")
    }

    fun lookUp(aktorId: String): JSONObject {
        val resp = khttp.get(
                url = "$aktorRegisteretUrl/api/v1/identer?gjeldende=true",
                headers = mapOf(
                        "Accept" to "application/json",
                        "Authorization" to "Bearer ${stsRestClient.token()}",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spenn",
                        "Nav-Personidenter" to aktorId
                ))
        if (resp.statusCode != 200) {
            LOG.error("Got statusCode ${resp.statusCode}")
        }
        try {
            return resp.jsonObject
        } catch (e:JSONException) {
            LOG.error("Bad JSON: ${resp.text}")
            throw e
        }
    }

}

class AktorNotFoundException(message: String, val aktorId: String) : Exception(message)

interface AktørTilFnrMapper {
    fun tilFnr(aktørId: String): Fodselsnummer
}
