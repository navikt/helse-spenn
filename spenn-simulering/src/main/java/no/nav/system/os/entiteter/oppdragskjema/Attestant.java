
package no.nav.system.os.entiteter.oppdragskjema;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="attestantId">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="8"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="datoUgyldigFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "attestantId",
    "datoUgyldigFom"
})
@XmlRootElement(name = "attestant")
public class Attestant {

    @XmlElement(required = true)
    protected String attestantId;
    protected String datoUgyldigFom;

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
     * Gets the value of the datoUgyldigFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoUgyldigFom() {
        return datoUgyldigFom;
    }

    /**
     * Sets the value of the datoUgyldigFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoUgyldigFom(String value) {
        this.datoUgyldigFom = value;
    }

}
