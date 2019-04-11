package no.nav.helse.spenn.oppslag

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.spenn.vedtak.Fodselsnummer
import org.slf4j.LoggerFactory
import java.util.*

class FnrOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(FnrOppslag::class.java.name)

    fun hentFnr(aktørId: String): Fodselsnummer {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/aktor/${aktørId}/fnr".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()

        return result.component1()!!
    }
}