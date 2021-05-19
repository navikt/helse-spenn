package no.nav.helse.spenn.utbetaling

import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

object OppdragXml {
    private val jaxbContext = JAXBContext.newInstance(Oppdrag::class.java)
    private val unmarshaller = jaxbContext.createUnmarshaller()
    private val marshaller = jaxbContext.createMarshaller().apply {
        setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    }

    private val xmlInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
    }

    fun marshal(oppdrag: Oppdrag): String {
        return StringWriter().use {
            marshaller.marshal(JAXBElement(QName("", "Oppdrag"), Oppdrag::class.java, oppdrag), it)
            it.toString()
        }
    }

    fun normalizeXml(oppdragXML: String) =
        oppdragXML
            .replace("<oppdrag xmlns=", "<ns2:oppdrag xmlns:ns2=", ignoreCase = true)
            .replace("</Oppdrag>", "</ns2:oppdrag>", ignoreCase = true)


    fun unmarshal(oppdragXML: String, normalize : Boolean = true): Oppdrag {
        return oppdragXML
            .let {if(normalize) normalizeXml(it) else it}
            .let {StringReader(it)}
            .use {
                xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
                unmarshaller.unmarshal(
                    xmlInputFactory.createXMLStreamReader(StreamSource(it)),
                    Oppdrag::class.java
                ).value
            }
    }

}
