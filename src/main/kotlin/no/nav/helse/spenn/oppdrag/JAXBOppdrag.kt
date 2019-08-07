
package no.nav.helse.spenn.oppdrag

import no.trygdeetaten.skjema.oppdrag.Oppdrag

import org.springframework.stereotype.Component
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

@Component
class JAXBOppdrag {

    val jaxbContext = JAXBContext.newInstance(OppdragSkjemaConstants.JAXB_CLASS)
    val unmarshaller = jaxbContext.createUnmarshaller()
    val marshaller = jaxbContext.createMarshaller().apply {
        setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    }

    val xmlInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
    }

    fun toOppdrag (oppdragXML : String) : Oppdrag {
        val source = StreamSource(StringReader(oppdragXML))
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
        val unmarshal = unmarshaller.unmarshal(xmlInputFactory.createXMLStreamReader(source),
                OppdragSkjemaConstants.JAXB_CLASS)
        return unmarshal.value
    }

    fun fromOppdragToXml(oppdrag: Oppdrag) : String {
        val stringWriter = StringWriter()
        marshaller.marshal(oppdrag, stringWriter)
        return stringWriter.toString()
    }

}
