package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

object OppdragXml {
    private val xmlMapper =
        XmlMapper
            .builder()
            .addModules(kotlinModule())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            // OS er sensitiv på rekkefølgen til elementene, så de må serialiseres i deklarasjonsrekkefølge
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // OS svarer tidvis med ugyldig XML, og normalizeXml kan legge på en ekstra avslutningstag. Ignorer alt etter rotelementet.
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            // gjør slik at jackson ikke serialiserer null-felter som tomme xml-felter, dvs. unngå `<mmel />` hvis `mmel` egentlig er null
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_EMPTY) }
            .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_EMPTY) }
            .build()

    fun marshal(oppdrag: OppdragDto): String =
        """<?xml version="1.0" encoding="utf-8"?>
${xmlMapper.writeValueAsString(oppdrag)}"""

    fun normalizeXml(oppdragXML: String): String {
        val medForventetÅpningstag = medForventetÅpningstag(oppdragXML)
        val medAvslutningstag = medForventetAvslutningstag(medForventetÅpningstag)
        return utenTomtOppdrag(medAvslutningstag)
    }

    // normaliserer åpningstag til lowercase, dvs. <OPPDRAG, <Oppdrag blir til <oppdrag
    private fun medForventetÅpningstag(xml: String) = xml.replace("<oppdrag", "<oppdrag", ignoreCase = true)

    private fun medForventetAvslutningstag(xml: String): String {
        if (!xml.contains("</oppdrag>", true)) return "$xml</oppdrag>"
        return xml.replace("</Oppdrag>", "</oppdrag>", ignoreCase = true)
    }

    private fun utenTomtOppdrag(xml: String) = xml.replace("<oppdrag-110></oppdrag-110>", "", ignoreCase = true)

    fun unmarshal(oppdragXML: String): KvitteringDto = xmlMapper.readValue<KvitteringDto>(normalizeXml(oppdragXML))
}
