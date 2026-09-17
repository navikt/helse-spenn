package com.github.navikt.tbd_libs.soap

import com.fasterxml.jackson.annotation.JsonRootName
import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.error
import com.github.navikt.tbd_libs.result_object.ok
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty
import tools.jackson.module.kotlin.readValue

inline fun <reified T> deserializeSoapBody(
    mapper: ObjectMapper,
    body: String,
): Result<SoapResult<T>> {
    val fault =
        try {
            mapper.readValue<SoapResponse<SoapFault>>(body).body.fault
        } catch (_: Exception) {
            null
        }
    return when (fault) {
        null ->
            try {
                when (val ting = mapper.readValue<SoapResponse<T?>>(body).body) {
                    null -> "SOAP Body er null".error()
                    else -> SoapResult.Ok(ting).ok()
                }
            } catch (err: Exception) {
                err.error("Klarte ikke tolke SOAP-responsen")
            }
        else -> SoapResult.Fault("SOAP fault: ${fault.code} - ${fault.messsage}", fault.detail?.toPrettyString()).ok()
    }
}

sealed interface SoapResult<out T> {
    data class Ok<T>(
        val response: T,
    ) : SoapResult<T>

    data class Fault(
        val message: String,
        val detalje: String?,
    ) : SoapResult<Nothing>
}

@JsonRootName(value = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
data class SoapResponse<T>(
    @param:JacksonXmlProperty(localName = "Header")
    val header: SoapHeader?,
    @param:JacksonXmlProperty(localName = "Body")
    val body: T,
)

data class SoapHeader(
    @param:JacksonXmlProperty(localName = "Action", namespace = "http://www.w3.org/2005/08/addressing")
    val action: String,
    @param:JacksonXmlProperty(localName = "MessageID", namespace = "http://www.w3.org/2005/08/addressing")
    val messageId: String,
    @param:JacksonXmlProperty(localName = "RelatesTo", namespace = "http://www.w3.org/2005/08/addressing")
    val relatesTo: String,
)

data class SoapFault(
    @param:JacksonXmlProperty(localName = "Fault", namespace = "http://www.w3.org/2003/05/soap-envelope")
    val fault: Fault,
)

data class Fault(
    @param:JacksonXmlProperty(localName = "faultcode")
    val code: String,
    @param:JacksonXmlProperty(localName = "faultstring")
    val messsage: String,
    @param:JacksonXmlProperty(localName = "detail")
    val detail: JsonNode?,
)
