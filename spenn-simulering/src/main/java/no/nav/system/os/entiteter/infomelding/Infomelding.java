
package no.nav.system.os.entiteter.infomelding;

import jakarta.xml.bind.annotation.*;


/**
 * Informasjonsmelding hentes fra ID 469MMEL
 * 
 * <p>Java class for infomelding complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="infomelding">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="beskrMelding">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="75"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "infomelding", propOrder = {
    "beskrMelding"
})
public class Infomelding {

    @XmlElement(required = true)
    protected String beskrMelding;

    /**
     * Gets the value of the beskrMelding property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBeskrMelding() {
        return beskrMelding;
    }

    /**
     * Sets the value of the beskrMelding property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBeskrMelding(String value) {
        this.beskrMelding = value;
    }

}
