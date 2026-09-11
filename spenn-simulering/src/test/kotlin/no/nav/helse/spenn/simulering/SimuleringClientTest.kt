package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.AzureToken
import com.github.navikt.tbd_libs.azure.AzureTokenProvider
import com.github.navikt.tbd_libs.mock.MockHttpResponse
import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.ok
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

class SimuleringClientTest {
    private companion object {
        private const val BRUKERNAVN = "srvspenn"
        private const val PASSORD = "et-passord"
    }

    @Test
    fun `sender serviceuser-legitimasjonen til simulering-api`() {
        val httpClient = mockHttpClient()
        val client = client(httpClient)

        client.hentSimulering(simuleringRequest(), "en-callid")

        verifiserHeader(httpClient, "X-ServiceUser-Username") { it == BRUKERNAVN }
        verifiserHeader(httpClient, "X-ServiceUser-Password") { it == PASSORD }
    }

    private fun verifiserHeader(
        httpClient: HttpClient,
        navn: String,
        verifisering: (String?) -> Boolean,
    ) {
        verify {
            httpClient.send<String>(
                match { request: HttpRequest -> verifisering(request.headers().firstValue(navn).getOrNull()) },
                any(),
            )
        }
    }

    private fun mockHttpClient() =
        mockk<HttpClient> {
            every { send<String>(any(), any()) } returns MockHttpResponse("", 204)
        }

    private fun client(
        httpClient: HttpClient,
        serviceuserUsername: String = BRUKERNAVN,
        serviceuserPassword: String = PASSORD,
    ) = SimuleringClient(
        httpClient = httpClient,
        objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
        tokenProvider =
            object : AzureTokenProvider {
                override fun onBehalfOfToken(
                    scope: String,
                    token: String,
                ): Result<AzureToken> = AzureToken("on_behalf_of_token", LocalDateTime.now()).ok()

                override fun bearerToken(scope: String): Result<AzureToken> = AzureToken("bearer_token", LocalDateTime.now()).ok()
            },
        serviceuserUsername = serviceuserUsername,
        serviceuserPassword = serviceuserPassword,
        baseUrl = "http://spenn-simulering-api",
        scope = "api://dev-fss.tbd.spenn-simulering-api/.default",
    )

    private fun simuleringRequest() =
        SimuleringRequest(
            fødselsnummer = "12345678911",
            oppdrag =
                SimuleringRequest.Oppdrag(
                    fagområde = SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON,
                    fagsystemId = "fagsystemId",
                    endringskode = SimuleringRequest.Oppdrag.Endringskode.NY,
                    mottakerAvUtbetalingen = "123456789",
                    linjer =
                        listOf(
                            SimuleringRequest.Oppdrag.Oppdragslinje(
                                endringskode = SimuleringRequest.Oppdrag.Endringskode.NY,
                                fom = LocalDate.of(2018, 1, 1),
                                tom = LocalDate.of(2018, 1, 20),
                                satstype = SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.DAGLIG,
                                sats = 500,
                                grad = 100,
                                delytelseId = 1,
                                refDelytelseId = null,
                                refFagsystemId = null,
                                klassekode = SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                                klassekodeFom = LocalDate.of(2018, 1, 1),
                                opphørerFom = null,
                            ),
                        ),
                ),
            maksdato = LocalDate.of(2018, 12, 31),
            saksbehandler = "saksbehandler",
        )
}
