package no.nav.helse.spenn.simulering.api.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.SoapAssertionStrategy
import com.github.navikt.tbd_libs.soap.SoapResult
import com.github.navikt.tbd_libs.soap.deserializeSoapBody
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class SimuleringV2Service(
    private val soapClient: MinimalSoapClient,
    private val assertionStrategy: SoapAssertionStrategy,
    private val mapper: XmlMapper = XmlMapper()
) {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SimuleringV2Service::class.java)
        private val jsonMapper = jacksonObjectMapper().registerModules(JavaTimeModule())
    }

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest): SimuleringResult {
        val requestBody = buildXmlRequestBody(simulerRequest)
        sikkerLogg.info("SimuleringV2 request:\n$requestBody")
        return when (val result = soapClient.doSoapAction("http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt/simulerFpService/simulerBeregningRequest", requestBody, assertionStrategy)) {
            is MinimalSoapClient.Result.Error -> {
                sikkerLogg.info("Feil ved simuleringV2: {}", result.error, result.exception)
                return SimuleringResult(
                    status = SimuleringStatus.TEKNISK_FEIL,
                    feilmelding = result.error
                )
            }
            is MinimalSoapClient.Result.Ok -> tolkRespons(result.body)
        }
    }

    private fun tolkRespons(responseBody: String): SimuleringResult {
        return when (val result = deserializeSoapBody<JsonNode>(mapper, responseBody)) {
            is SoapResult.Fault -> håndterFault(result)
            is SoapResult.InvalidResponse -> {
                sikkerLogg.info("Feil ved simuleringV2: ${result.exception?.message}. Oppdrag/OS er trolig stengt.", result.exception)
                sikkerLogg.info("Response body:\n${result.responseBody}")
                return SimuleringResult(
                    status = SimuleringStatus.OPPDRAG_UR_ER_STENGT,
                    feilmelding = result.exception?.message
                )
            }
            is SoapResult.Ok -> {
                sikkerLogg.info("Simuleringsrespons fra oppdrag:\n${result.response.toPrettyString()}")
                mapResponseToResultat(result.response.path("simulerBeregningResponse").path("response").path("simulering"))
            }
        }
    }

    private fun håndterFault(err: SoapResult.Fault): SimuleringResult {
        val detalje = err.detalje ?: return håndterGenerellFault(err)
        return tolkOppdragFault(err, detalje)
    }

    private fun håndterGenerellFault(err: SoapResult.Fault): SimuleringResult {
        sikkerLogg.info("SOAP FAULT: ${err.message}")
        return SimuleringResult(
            status = SimuleringStatus.FUNKSJONELL_FEIL,
            feilmelding = err.message
        )
    }

    private fun tolkOppdragFault(fault: SoapResult.Fault, detalje: String): SimuleringResult {
        val node = tolkDetaljeSomJson(detalje) ?: return håndterGenerellFault(fault)
        return tolkJsonSomOppdragFault(node) ?: return håndterGenerellFault(fault)
    }

    private fun tolkDetaljeSomJson(detalje: String): ObjectNode? {
        val node = try {
            jsonMapper.readTree(detalje)
        } catch (_: Exception) {
            return null
        }
        if (node !is ObjectNode) return null
        return node
    }

    private fun tolkJsonSomOppdragFault(node: ObjectNode): SimuleringResult? {
        val feiltype = node.fieldNames().next()
        val fault = node.path(feiltype)
        try {
            return when (feiltype) {
                "CICSFault" -> håndterCicsFault(fault)
                else -> håndterOppdragFault(feiltype, fault)
            }
        } catch (err: Exception) {
            sikkerLogg.info("Kunne ikke oversette til oppdrag fault. feiltype={}, innhold={} fordi {}", feiltype, fault.toPrettyString(), err.message, err)
            return null
        }
    }

    private fun håndterOppdragFault(feiltype: String, fault: JsonNode): SimuleringResult {
        val oppdragFault = jsonMapper.convertValue<Oppdragfault>(fault)
        sikkerLogg.info("Feil fra OS: {}:\nMelding: {}\nKilde: {}\nRotårsak: {}\nTidspunkt: {}", feiltype, oppdragFault.melding, oppdragFault.kilde, oppdragFault.rotårsak, oppdragFault.tidspunkt)
        return SimuleringResult(
            status = SimuleringStatus.FUNKSJONELL_FEIL,
            feilmelding = oppdragFault.melding
        )
    }
    private fun håndterCicsFault(fault: JsonNode): SimuleringResult {
        Oppdragfault(fault.asText(), "", "", LocalDateTime.now())
        sikkerLogg.info("Teknisk feil fra OS: {}", fault.asText())
        return SimuleringResult(
            status = SimuleringStatus.TEKNISK_FEIL,
            feilmelding = fault.asText()
        )
    }

    private fun mapResponseToResultat(simulering: JsonNode) = SimuleringResult(
        status = SimuleringStatus.OK,
        simulering = if (simulering.isNull || simulering.isMissingNode) null else Simulering(
            gjelderId = simulering.path("gjelderId").asText(),
            gjelderNavn = simulering.path("gjelderNavn").asText().trim(),
            datoBeregnet = LocalDate.parse(simulering.path("datoBeregnet").asText()),
            totalBelop = simulering.path("belop").asInt(),
            periodeList = simulering.path("beregningsPeriode").asArray().map { mapBeregningsPeriode(it) }
        )
    )

    private fun JsonNode.asArray() = when (this) {
        is ObjectNode -> jsonMapper.createArrayNode().add(this)
        else -> this
    }

    private fun mapBeregningsPeriode(periode: JsonNode) =
        SimulertPeriode(
            fom = LocalDate.parse(periode.path("periodeFom").asText()),
            tom = LocalDate.parse(periode.path("periodeTom").asText()),
            utbetaling = periode.path("beregningStoppnivaa").asArray().map { mapBeregningStoppNivaa(it) }
        )

    private fun mapBeregningStoppNivaa(stoppNivaa: JsonNode) =
        Utbetaling(
            fagSystemId = stoppNivaa.path("fagsystemId").asText().trim(),
            utbetalesTilNavn = stoppNivaa.path("utbetalesTilNavn").asText().trim(),
            utbetalesTilId = stoppNivaa.path("utbetalesTilId").asText().removePrefix("00"),
            forfall = LocalDate.parse(stoppNivaa.path("forfall").asText()),
            feilkonto = stoppNivaa.path("feilkonto").asBoolean(),
            detaljer = stoppNivaa.path("beregningStoppnivaaDetaljer").asArray().map { mapDetaljer(it) })

    private fun mapDetaljer(detaljer: JsonNode) =
        Detaljer(
            faktiskFom = LocalDate.parse(detaljer.path("faktiskFom").asText()),
            faktiskTom = LocalDate.parse(detaljer.path("faktiskTom").asText()),
            uforegrad = detaljer.path("uforeGrad").asInt(),
            antallSats = detaljer.path("antallSats").asInt(),
            typeSats = detaljer.path("typeSats").asText().trim(),
            sats = detaljer.path("sats").asDouble(),
            belop = detaljer.path("belop").asInt(),
            konto = detaljer.path("kontoStreng").asText().trim(),
            tilbakeforing = detaljer.path("tilbakeforing").asBoolean(),
            klassekode = detaljer.path("klassekode").asText().trim(),
            klassekodeBeskrivelse = detaljer.path("klasseKodeBeskrivelse").asText().trim(),
            utbetalingsType = detaljer.path("typeKlasse").asText(),
            refunderesOrgNr = detaljer.path("refunderesOrgNr").asText().removePrefix("00")
        )

    private data class Oppdragfault(
        @JsonProperty("errorMessage")
        val melding: String,
        @JsonProperty("errorSource")
        val kilde: String,
        @JsonProperty("rootCause")
        val rotårsak: String,
        @JsonProperty("dateTimeStamp")
        val tidspunkt: LocalDateTime
    )

    @Language("XML")
    private fun buildXmlRequestBody(request: SimulerBeregningRequest): String {
        return """<ns2:simulerBeregningRequest xmlns:ns2="http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt"
                             xmlns:ns3="http://nav.no/system/os/entiteter/oppdragSkjema">
    <request>
        <simuleringsPeriode>
            <datoSimulerFom>${request.simuleringsPeriode.datoSimulerFom}</datoSimulerFom>
            <datoSimulerTom>${request.simuleringsPeriode.datoSimulerTom}</datoSimulerTom>
        </simuleringsPeriode>
        <oppdrag>
            <kodeEndring>${request.oppdrag.kodeEndring}</kodeEndring>
            <kodeFagomraade>${request.oppdrag.kodeFagomraade}</kodeFagomraade>
            <fagsystemId>${request.oppdrag.fagsystemId}</fagsystemId>
            <utbetFrekvens>${request.oppdrag.utbetFrekvens}</utbetFrekvens>
            <oppdragGjelderId>${request.oppdrag.oppdragGjelderId}</oppdragGjelderId>
            <datoOppdragGjelderFom>${request.oppdrag.datoOppdragGjelderFom}</datoOppdragGjelderFom>
            <saksbehId>${request.oppdrag.saksbehId}</saksbehId>
            ${request.oppdrag.enhet.joinToString(separator = "\n") { enhet ->
                """<ns3:enhet>
                    <typeEnhet>${enhet.typeEnhet}</typeEnhet>
                    <enhet>${enhet.enhet}</enhet>
                    <datoEnhetFom>${enhet.datoEnhetFom}</datoEnhetFom>
                </ns3:enhet>"""    
            }}
            ${request.oppdrag.oppdragslinje.joinToString(separator = "\n") { linje ->
                """<oppdragslinje>
                <kodeEndringLinje>${linje.kodeEndringLinje}</kodeEndringLinje>
                <delytelseId>${linje.delytelseId}</delytelseId>
                ${linje.refDelytelseId?.let { """<refDelytelseId>${linje.refDelytelseId}</refDelytelseId>""" } ?: ""}
                ${linje.refFagsystemId?.let { """<refFagsystemId>${linje.refFagsystemId}</refFagsystemId>""" } ?: ""}
                <kodeKlassifik>${linje.kodeKlassifik}</kodeKlassifik>
                ${linje.kodeStatusLinje?.let { """<kodeStatusLinje>${linje.kodeStatusLinje}</kodeStatusLinje>""" } ?: ""}
                ${linje.datoStatusFom?.let { """<datoStatusFom>${linje.datoStatusFom}</datoStatusFom>""" } ?: ""}
                <datoVedtakFom>${linje.datoVedtakFom}</datoVedtakFom>
                <datoVedtakTom>${linje.datoVedtakTom}</datoVedtakTom>
                <sats>${linje.sats}</sats>
                <fradragTillegg>${linje.fradragTillegg}</fradragTillegg>
                <typeSats>${linje.typeSats}</typeSats>
                <brukKjoreplan>${linje.brukKjoreplan}</brukKjoreplan>
                <saksbehId>${linje.saksbehId}</saksbehId>
                ${linje.utbetalesTilId?.let {
                    """<utbetalesTilId>$it</utbetalesTilId>"""
                } ?: ""}
                ${linje.grad.joinToString(separator = "\n") { grad ->
                    """<ns3:grad>
                    <typeGrad>${grad.typeGrad}</typeGrad>
                    ${grad.grad?.let { """<grad>${grad.grad}</grad>""" } ?: ""}
                </ns3:grad>"""
                }}
                ${linje.attestant.joinToString(separator = "\n") { attestant ->
                    """<ns3:attestant>
                    <attestantId>${attestant.attestantId}</attestantId>
                </ns3:attestant>"""
                }}
                ${linje.refusjonsInfo?.let { refusjonsInfo ->
                    """<ns3:refusjonsInfo>
                    <refunderesId>${refusjonsInfo.refunderesId}</refunderesId>
                    ${refusjonsInfo.maksDato?.let { """<maksDato>${refusjonsInfo.maksDato}</maksDato>""" } ?: ""}
                    <datoFom>${refusjonsInfo.datoFom}</datoFom>
                </ns3:refusjonsInfo>"""
                } ?: "" }
            </oppdragslinje>"""
            }}
        </oppdrag>
    </request>
</ns2:simulerBeregningRequest>"""
    }
}
