package no.nav.helse.spenn.simulering

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.soap.MinimalSoapClient
import com.github.navikt.tbd_libs.soap.SoapAssertionStrategy
import com.github.navikt.tbd_libs.soap.SoaptjenesteException
import com.github.navikt.tbd_libs.soap.deserializeSoapBody
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
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

    fun simulerOppdrag(simulerRequest: SimulerBeregningRequest) {
        val requestBody = buildXmlRequestBody(simulerRequest)
        sikkerLogg.info("SimuleringV2 request:\n$requestBody")
        try {
            val responseBody = soapClient.doSoapAction("http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceGrensesnitt/simulerFpService/simulerBeregningRequest", requestBody, assertionStrategy)
            tolkRespons(responseBody)
        } catch (err: Exception) {
            sikkerLogg.info("Feil ved simuleringV2: {}", err.message, err)
        }
    }

    private fun tolkRespons(responseBody: String) {
        try {
            val result = deserializeSoapBody<JsonNode>(mapper, responseBody)
            sikkerLogg.info("Simuleringsrespons fra oppdrag:\n${result.toPrettyString()}")
        } catch (err: SoaptjenesteException) {
            håndterFault(err)
        }
    }

    private fun håndterFault(err: SoaptjenesteException) {
        val detalje = err.detalje ?: return håndterGenerellFault(err)
        tolkOppdragFault(err, detalje)
    }

    private fun håndterGenerellFault(err: SoaptjenesteException) {
        sikkerLogg.info("SOAP FAULT: ${err.message}")
    }

    private fun tolkOppdragFault(fault: SoaptjenesteException, detalje: String) {
        val node = tolkDetaljeSomJson(detalje) ?: return håndterGenerellFault(fault)
        val (feiltype, oppdragFault) = tolkJsonSomOppdragFault(node) ?: return håndterGenerellFault(fault)
        sikkerLogg.info("Feil fra OS: {}:\nMelding: {}\nKilde: {}\nRotårsak: {}\nTidspunkt: {}", feiltype, oppdragFault.melding, oppdragFault.kilde, oppdragFault.rotårsak, oppdragFault.tidspunkt)
    }

    private fun tolkDetaljeSomJson(detalje: String): ObjectNode? {
        val node = try {
            jsonMapper.readTree(detalje)
        } catch (err: Exception) {
            return null
        }
        if (node !is ObjectNode) return null
        return node
    }

    private fun tolkJsonSomOppdragFault(node: ObjectNode): Pair<String, Oppdragfault>? {
        val feiltype = node.fieldNames().next()
        try {
            return feiltype to jsonMapper.convertValue<Oppdragfault>(node.path(feiltype))
        } catch (err: Exception) {
            sikkerLogg.info("Kunne ikke oversette til oppdrag fault. feiltype={}, innhold={} fordi {}", feiltype, node.path(feiltype).toPrettyString(), err.message, err)
            return null
        }
    }

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
            <datoSimulerFom>${request.request.simuleringsPeriode.datoSimulerFom}</datoSimulerFom>
            <datoSimulerTom>${request.request.simuleringsPeriode.datoSimulerTom}</datoSimulerTom>
        </simuleringsPeriode>
        <oppdrag>
            <kodeEndring>${request.request.oppdrag.kodeEndring}</kodeEndring>
            <kodeFagomraade>${request.request.oppdrag.kodeFagomraade}</kodeFagomraade>
            <fagsystemId>${request.request.oppdrag.fagsystemId}</fagsystemId>
            <utbetFrekvens>${request.request.oppdrag.utbetFrekvens}</utbetFrekvens>
            <oppdragGjelderId>${request.request.oppdrag.oppdragGjelderId}</oppdragGjelderId>
            <datoOppdragGjelderFom>${request.request.oppdrag.datoOppdragGjelderFom}</datoOppdragGjelderFom>
            <saksbehId>${request.request.oppdrag.saksbehId}</saksbehId>
            ${request.request.oppdrag.enhet.joinToString(separator = "\n") { enhet ->
                """<ns3:enhet>
                    <typeEnhet>${enhet.typeEnhet}</typeEnhet>
                    <enhet>${enhet.enhet}</enhet>
                    <datoEnhetFom>${enhet.datoEnhetFom}</datoEnhetFom>
                </ns3:enhet>"""    
            }}
            ${request.request.oppdrag.oppdragslinje.joinToString(separator = "\n") { linje ->
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
                
                ${linje.utbetalesTilId?.let { 
                    """<utbetalesTilId>$it</utbetalesTilId>"""
                } ?: ""}
                
            </oppdragslinje>"""
            }}
        </oppdrag>
    </request>
</ns2:simulerBeregningRequest>"""
    }
}
