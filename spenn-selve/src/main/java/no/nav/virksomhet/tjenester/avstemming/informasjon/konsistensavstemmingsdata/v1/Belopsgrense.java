
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;


/**
 * <p>Java class for Belopsgrense complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Belopsgrense">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="grenseType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="belopsgrense" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/>
 *         &lt;element name="grensePeriode" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Periode" minOccurs="0"/>
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
@XmlType(name = "Belopsgrense", propOrder = {
    "grenseType",
    "belopsgrense",
    "grensePeriode",
    "feilregistrering"
})
public class Belopsgrense {

    protected String grenseType;
    protected BigDecimal belopsgrense;
    protected Periode grensePeriode;
    protected String feilregistrering;

    /**
     * Gets the value of the grenseType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGrenseType() {
        return grenseType;
    }

    /**
     * Sets the value of the grenseType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGrenseType(String value) {
        this.grenseType = value;
    }

    /**
     * Gets the value of the belopsgrense property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getBelopsgrense() {
        return belopsgrense;
    }

    /**
     * Sets the value of the belopsgrense property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setBelopsgrense(BigDecimal value) {
        this.belopsgrense = value;
    }

    /**
     * Gets the value of the grensePeriode property.
     * 
     * @return
     *     possible object is
     *     {@link Periode }
     *     
     */
    public Periode getGrensePeriode() {
        return grensePeriode;
    }

    /**
     * Sets the value of the grensePeriode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Periode }
     *     
     */
    public void setGrensePeriode(Periode value) {
        this.grensePeriode = value;
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
