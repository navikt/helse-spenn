package no.nav.helse.spenn.grensesnittavstemming


import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.springframework.stereotype.Component
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

@Component
class JAXBAvstemmingsdata {

    private final val jaxbContext:JAXBContext = JAXBContext.newInstance(AvstemmingSkjemaConstants.JAXB_CLASS)
    private val unmarshaller:Unmarshaller = jaxbContext.createUnmarshaller()
    private val marshaller:Marshaller = jaxbContext.createMarshaller().apply {
        setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    }

    private val xmlInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
    }

    fun toAvstemmingsdata (avstemmingsdataXML : String) : Avstemmingsdata {
        val source = StreamSource(StringReader(avstemmingsdataXML))
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
        val unmarshal = unmarshaller.unmarshal(xmlInputFactory.createXMLStreamReader(source),
                AvstemmingSkjemaConstants.JAXB_CLASS)
        return unmarshal.value
    }

    fun fromAvstemmingsdataToXml(avstemmingsdata: Avstemmingsdata) : String {
        val stringWriter = StringWriter()
        marshaller.marshal(avstemmingsdata, stringWriter)
        return stringWriter.toString()
    }

}
