package no.nav.helse.spenn.simulering.api

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.engine.connector
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.spenn.simulering.api.client.Simulering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.time.Instant
import java.time.LocalDate
import java.util.Date

class E2ETest {

    @Test
    fun test() {
        val simuleringsvar = Simulering(
            gjelderId = "fnr",
            gjelderNavn = "navn",
            datoBeregnet = LocalDate.now(),
            totalBelop = 0,
            periodeList = emptyList()
        )
        val simuleringtjeneste = mockk<Simuleringtjeneste> {
            every { simulerOppdrag(any()) } returns SimuleringResponse.Ok(simuleringsvar)
        }

        simuleringTestApp(simuleringtjeneste) {
            val request = SimuleringRequest(
                fødselsnummer = "fnr",
                oppdrag = SimuleringRequest.Oppdrag(
                    fagområde = SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON,
                    fagsystemId = "fagsystemId",
                    endringskode = SimuleringRequest.Oppdrag.Endringskode.NY,
                    mottakerAvUtbetalingen = "orgnr",
                    linjer = emptyList()
                ),
                maksdato = LocalDate.now(),
                saksbehandler = "saksbehandler",
            )
            val response = sendSimuleringRequest(request)
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.body<ForventetSimuleringResponse>()
            assertEquals(simuleringsvar.gjelderId, body.gjelderId)
            assertEquals(simuleringsvar.gjelderNavn, body.gjelderNavn)
            assertEquals(simuleringsvar.datoBeregnet, body.datoBeregnet)
            assertEquals(simuleringsvar.totalBelop, body.totalBelop)
        }
    }

    private fun simuleringTestApp(simuleringtjeneste: Simuleringtjeneste, testblokk: suspend TestContext.() -> Unit) {
        val innloggetBruker = JWTPrincipal(LokalePayload(mapOf(
            "azp_name" to "spenn-simulering"
        )))
        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        simuleringApp(simuleringtjeneste, objectMapper, CollectorRegistry.defaultRegistry, innloggetBruker, testblokk)
    }

    private fun simuleringApp(simuleringtjeneste: Simuleringtjeneste, objectMapper: ObjectMapper, collectorRegistry: CollectorRegistry, innloggetBruker: JWTPrincipal, testblokk: suspend TestContext.() -> Unit) {
        val randomPort = ServerSocket(0).localPort
        testApplication {
            environment {
                connector {
                    host = "localhost"
                    port = randomPort
                }
            }
            application {
                authentication {
                    provider {
                        authenticate { context ->
                            innloggetBruker
                        }
                    }

                }
                lagApplikasjonsmodul(simuleringtjeneste, objectMapper, collectorRegistry)
            }
            startApplication()

            val testClient = createClient {
                defaultRequest {
                    port = randomPort
                }
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            do {
                val response = testClient.get("/isready")
                println("Venter på at isready svarer OK…:${response.status}")
            } while (!response.status.isSuccess())

            testblokk(TestContext(testClient))
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForventetSimuleringResponse(
    val gjelderId: String,
    val gjelderNavn: String,
    val datoBeregnet: LocalDate,
    val totalBelop: Int,
    val periodeList: List<SimulertPeriode>
) {
    data class SimulertPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetaling: List<Utbetaling>
    )

    data class Utbetaling(
        val fagSystemId: String,
        val utbetalesTilId: String,
        val utbetalesTilNavn: String,
        val forfall: LocalDate,
        val feilkonto: Boolean,
        val detaljer: List<Detaljer>
    )

    data class Detaljer(
        val faktiskFom: LocalDate,
        val faktiskTom: LocalDate,
        val konto: String,
        val belop: Int,
        val tilbakeforing: Boolean,
        val sats: Double,
        val typeSats: String,
        val antallSats: Int,
        val uforegrad: Int,
        val klassekode: String,
        val klassekodeBeskrivelse: String,
        val utbetalingsType: String,
        val refunderesOrgNr: String
    )
}

class TestContext(val client: HttpClient) {

    suspend fun sendSimuleringRequest(simuleringRequest: SimuleringRequest): HttpResponse {
        return client.post("/api/simulering") {
            contentType(ContentType.Application.Json)
            setBody(simuleringRequest)
        }
    }
}

class LokalePayload(claims: Map<String, String>) : Payload {
    private val claims = claims.mapValues { LokaleClaim(it.value) }
    override fun getIssuer(): String {
        return "lokal utsteder"
    }

    override fun getSubject(): String {
        return "lokal subjekt"
    }

    override fun getAudience(): List<String> {
        return listOf("lokal publikum")
    }

    override fun getExpiresAt(): Date {
        return Date.from(Instant.MAX)
    }

    override fun getNotBefore(): Date {
        return Date.from(Instant.EPOCH)
    }

    override fun getIssuedAt(): Date {
        return Date.from(Instant.now())
    }

    override fun getId(): String {
        return "lokal id"
    }

    override fun getClaim(name: String): Claim {
        return claims.getValue(name)
    }

    override fun getClaims(): Map<String, Claim> {
        return claims
    }
}

private class LokaleClaim(private val verdi: String) : Claim {
    override fun isNull() = false
    override fun isMissing() = false
    override fun asBoolean() = true
    override fun asInt() = 0
    override fun asLong() = 0L
    override fun asDouble() = 0.0
    override fun asString() = verdi
    override fun asDate() = Date.from(Instant.EPOCH)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> asArray(clazz: Class<T>?) = emptyArray<Any>() as Array<T>
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> asList(clazz: Class<T>?) = emptyList<Any>() as List<T>
    override fun asMap() = emptyMap<String, Any>()
    override fun <T : Any?> `as`(clazz: Class<T>?) = throw NotImplementedError()
}