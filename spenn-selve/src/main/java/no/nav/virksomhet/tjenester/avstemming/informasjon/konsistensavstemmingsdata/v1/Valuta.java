
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Valuta complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Valuta">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="valutaType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="valuta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="valutaFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="feilregistrering" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Valuta", propOrder = {
    "valutaType",
    "valuta",
    "valutaFom",
    "feilregistrering"
})
public class Valuta {

    protected String valutaType;
    protected String valuta;
    protected String valutaFom;
    protected String feilregistrering;

    /**
     * Gets the value of the valutaType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValutaType() {
        return valutaType;
    }

    /**
     * Sets the value of the valutaType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValutaType(String value) {
        this.valutaType = value;
    }

    /**
     * Gets the value of the valuta property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValuta() {
        return valuta;
    }

    /**
     * Sets the value of the valuta property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValuta(String value) {
        this.valuta = value;
    }

    /**
     * Gets the value of the valutaFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValutaFom() {
        return valutaFom;
    }

    /**
     * Sets the value of the valutaFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValutaFom(String value) {
        this.valutaFom = value;
    }

    /**
     * Gets the value of the feilregistrering property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFeilregistrering() {
        return feilregistrering;
    }

    /**
     * Sets the value of the feilregistrering property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFeilregistrering(String value) {
        this.feilregistrering = value;
    }

}
