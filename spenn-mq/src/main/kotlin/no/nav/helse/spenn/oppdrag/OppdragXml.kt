package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object OppdragXml {
    private val xmlMapper = XmlMapper.builder()
        .addModules(kotlinModule())
        .addModules(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        // gjør slik at jackson ikke serialiserer null-felter som tomme xml-felter, dvs. unngå `<mmel />` hvis `mmel` egentlig er null
        .serializationInclusion(JsonInclude.Include.NON_EMPTY)
        .build()

    fun marshal(oppdrag: OppdragDto): String {
        return xmlMapper.writeValueAsString(oppdrag)
    }

    fun normalizeXml(oppdragXML: String) =
        oppdragXML.replace("</Oppdrag>", "</oppdrag>")


    fun unmarshal(oppdragXML: String): OppdragDto {
        return xmlMapper.readValue<OppdragDto>(oppdragXML)
    }

}
