
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Periode complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Periode">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="tom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Periode", propOrder = {
    "fom",
    "tom"
})
public class Periode {

    protected String fom;
    protected String tom;

    /**
     * Gets the value of the fom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFom() {
        return fom;
    }

    /**
     * Sets the value of the fom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFom(String value) {
        this.fom = value;
    }

    /**
     * Gets the value of the tom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTom() {
        return tom;
    }

    /**
     * Sets the value of the tom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTom(String value) {
        this.tom = value;
    }

}
