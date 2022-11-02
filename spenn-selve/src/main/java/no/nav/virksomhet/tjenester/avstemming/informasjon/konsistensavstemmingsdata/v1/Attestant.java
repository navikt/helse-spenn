
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Attestant complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Attestant">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="attestantId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ugyldigFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Attestant", propOrder = {
    "attestantId",
    "ugyldigFom"
})
public class Attestant {

    protected String attestantId;
    protected String ugyldigFom;

    /**
     * Gets the value of the attestantId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttestantId() {
        return attestantId;
    }

    /**
     * Sets the value of the attestantId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttestantId(String value) {
        this.attestantId = value;
    }

    /**
     * Gets the value of the ugyldigFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUgyldigFom() {
        return ugyldigFom;
    }

    /**
     * Sets the value of the ugyldigFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUgyldigFom(String value) {
        this.ugyldigFom = value;
    }

}
