package no.nav.helse.spenn.oppdrag


import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

class AvstemmingdataXml {

    private val jaxbContext = JAXBContext.newInstance(Avstemmingsdata::class.java)
    private val unmarshaller = jaxbContext.createUnmarshaller()
    private val marshaller = jaxbContext.createMarshaller().apply {
        setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    }

    private val xmlInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
    }

    fun marshal(avstemmingsdata: Avstemmingsdata) : String {
        return StringWriter().use {
            marshaller.marshal(avstemmingsdata, it)
            it.toString()
        }
    }

    fun unmarshal(avstemmingsdataXML: String) : Avstemmingsdata {
        return StringReader(avstemmingsdataXML).use {
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
            unmarshaller.unmarshal(
                xmlInputFactory.createXMLStreamReader(StreamSource(it)),
                Avstemmingsdata::class.java
            ).value
        }
    }

}
