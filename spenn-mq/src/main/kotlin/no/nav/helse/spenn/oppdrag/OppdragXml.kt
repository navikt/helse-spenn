package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

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
        return """<?xml version="1.0" encoding="utf-8"?>
${xmlMapper.writeValueAsString(oppdrag)}"""
    }

    fun normalizeXml(oppdragXML: String): String {
        val medForventetÅpningstag = medForventetÅpningstag(oppdragXML)
        val medAvslutningstag = medForventetAvslutningstag(medForventetÅpningstag)
        return medForventetDatoformat(utenTomtOppdrag(medAvslutningstag))
    }

    // normaliserer åpningstag til lowercase, dvs. <OPPDRAG, <Oppdrag blir til <oppdrag
    private fun medForventetÅpningstag(xml: String) = xml.replace("<oppdrag", "<oppdrag", ignoreCase = true)
    private fun medForventetAvslutningstag(xml: String): String {
        if (!xml.contains("</oppdrag>", true)) return  "$xml</oppdrag>"
        return xml.replace("</Oppdrag>", "</oppdrag>", ignoreCase = true)
    }
    private fun utenTomtOppdrag(xml: String) =
        xml.replace("<oppdrag-110></oppdrag-110>", "", ignoreCase = true)

    private fun medForventetDatoformat(xml: String): String {
        return tidspunkter(datoUtenTidssone(xml))
    }

    private fun tidspunkter(xml: String): String {
        // fikser tidspunkter, f.eks. 2019-09-20 13.31.28.572227 ==> 2019-09-20T13:31:28.572227
        val r = """(\d{4}-\d{2}-\d{2}) (\d{1,2})\.(\d{1,2})\.(\d+\.\d+)""".toRegex()
        return xml.replace(r, "$1T$2:$3:$4")
    }

    private fun datoUtenTidssone(xml: String): String {
        // erstatter dato med tidssone, f.eks. 1970-01-01+01:00 ==> 1970-01-01
        val r = """(\d{4}-\d{2}-\d{2})[+-][0-9:]{4,5}""".toRegex()
        return xml.replace(r, "$1")
    }

    fun unmarshal(oppdragXML: String): KvitteringDto {
        return xmlMapper.readValue<KvitteringDto>(normalizeXml(oppdragXML))
    }

}
