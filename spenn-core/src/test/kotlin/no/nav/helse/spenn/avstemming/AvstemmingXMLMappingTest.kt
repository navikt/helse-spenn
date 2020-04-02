package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.oppdrag.AvstemmingMapper
import no.nav.helse.spenn.oppdrag.AvstemmingdataXml
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AvstemmingXMLMappingTest {

    @Test
    fun testThatJAXBAvstemmingsdataIsAlive() {
        val avstemmingsdata = AvstemmingMapper.objectFactory.createAvstemmingsdata()
        val generertXml = AvstemmingdataXml().marshal(avstemmingsdata)

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:avstemmingsdata xmlns:ns2=\"http://nav.no/virksomhet/tjenester/avstemming/meldinger/v1\"/>\n", generertXml)

        val xmlFraAvstemmingsdataFraGenerertXML = AvstemmingdataXml()
            .marshal(AvstemmingdataXml().unmarshal(generertXml))

        assertEquals(generertXml, xmlFraAvstemmingsdataFraGenerertXML)
    }
}
