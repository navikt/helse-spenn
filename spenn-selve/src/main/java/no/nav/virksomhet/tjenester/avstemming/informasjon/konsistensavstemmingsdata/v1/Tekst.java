
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Tekst complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Tekst">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="tekstlinjeNummer" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="tekstKode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="tekst" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="tekstPeriode" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Periode" minOccurs="0"/>
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
@XmlType(name = "Tekst", propOrder = {
    "tekstlinjeNummer",
    "tekstKode",
    "tekst",
    "tekstPeriode",
    "feilregistrering"
})
public class Tekst {

    protected Integer tekstlinjeNummer;
    protected String tekstKode;
    protected String tekst;
    protected Periode tekstPeriode;
    protected String feilregistrering;

    /**
     * Gets the value of the tekstlinjeNummer property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTekstlinjeNummer() {
        return tekstlinjeNummer;
    }

    /**
     * Sets the value of the tekstlinjeNummer property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTekstlinjeNummer(Integer value) {
        this.tekstlinjeNummer = value;
    }

    /**
     * Gets the value of the tekstKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTekstKode() {
        return tekstKode;
    }

    /**
     * Sets the value of the tekstKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTekstKode(String value) {
        this.tekstKode = value;
    }

    /**
     * Gets the value of the tekst property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTekst() {
        return tekst;
    }

    /**
     * Sets the value of the tekst property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTekst(String value) {
        this.tekst = value;
    }

    /**
     * Gets the value of the tekstPeriode property.
     * 
     * @return
     *     possible object is
     *     {@link Periode }
     *     
     */
    public Periode getTekstPeriode() {
        return tekstPeriode;
    }

    /**
     * Sets the value of the tekstPeriode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Periode }
     *     
     */
    public void setTekstPeriode(Periode value) {
        this.tekstPeriode = value;
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
