
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for SendAsynkronKonsistensavstemmingsdataRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SendAsynkronKonsistensavstemmingsdataRequest">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="konsistensavstemmingsdata" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Konsistensavstemmingsdata"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SendAsynkronKonsistensavstemmingsdataRequest", propOrder = {
    "konsistensavstemmingsdata"
})
public class SendAsynkronKonsistensavstemmingsdataRequest {

    @XmlElement(required = true)
    protected Konsistensavstemmingsdata konsistensavstemmingsdata;

    /**
     * Gets the value of the konsistensavstemmingsdata property.
     * 
     * @return
     *     possible object is
     *     {@link Konsistensavstemmingsdata }
     *     
     */
    public Konsistensavstemmingsdata getKonsistensavstemmingsdata() {
        return konsistensavstemmingsdata;
    }

    /**
     * Sets the value of the konsistensavstemmingsdata property.
     * 
     * @param value
     *     allowed object is
     *     {@link Konsistensavstemmingsdata }
     *     
     */
    public void setKonsistensavstemmingsdata(Konsistensavstemmingsdata value) {
        this.konsistensavstemmingsdata = value;
    }

}
