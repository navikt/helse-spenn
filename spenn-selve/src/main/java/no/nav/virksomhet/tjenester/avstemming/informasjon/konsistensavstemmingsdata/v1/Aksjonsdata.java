
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;


/**
 * Enhver avstemming må initieres og avsluttes med en 110-record, på det formatet som er beskrevet her
 * 
 * <p>Java class for Aksjonsdata complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Aksjonsdata">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="aksjonsType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="kildeType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="avstemmingType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="avleverendeKomponentKode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="mottakendeKomponentKode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="underkomponentKode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="nokkelFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="nokkelTom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="tidspunktAvstemmingTom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="avleverendeAvstemmingId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="brukerId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Aksjonsdata", propOrder = {
    "aksjonsType",
    "kildeType",
    "avstemmingType",
    "avleverendeKomponentKode",
    "mottakendeKomponentKode",
    "underkomponentKode",
    "nokkelFom",
    "nokkelTom",
    "tidspunktAvstemmingTom",
    "avleverendeAvstemmingId",
    "brukerId"
})
public class Aksjonsdata {

    @XmlElement(required = true)
    protected String aksjonsType;
    @XmlElement(required = true)
    protected String kildeType;
    @XmlElement(required = true)
    protected String avstemmingType;
    @XmlElement(required = true)
    protected String avleverendeKomponentKode;
    @XmlElement(required = true)
    protected String mottakendeKomponentKode;
    protected String underkomponentKode;
    protected String nokkelFom;
    protected String nokkelTom;
    protected String tidspunktAvstemmingTom;
    @XmlElement(required = true)
    protected String avleverendeAvstemmingId;
    @XmlElement(required = true)
    protected String brukerId;

    /**
     * Gets the value of the aksjonsType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAksjonsType() {
        return aksjonsType;
    }

    /**
     * Sets the value of the aksjonsType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAksjonsType(String value) {
        this.aksjonsType = value;
    }

    /**
     * Gets the value of the kildeType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKildeType() {
        return kildeType;
    }

    /**
     * Sets the value of the kildeType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKildeType(String value) {
        this.kildeType = value;
    }

    /**
     * Gets the value of the avstemmingType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvstemmingType() {
        return avstemmingType;
    }

    /**
     * Sets the value of the avstemmingType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvstemmingType(String value) {
        this.avstemmingType = value;
    }

    /**
     * Gets the value of the avleverendeKomponentKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvleverendeKomponentKode() {
        return avleverendeKomponentKode;
    }

    /**
     * Sets the value of the avleverendeKomponentKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvleverendeKomponentKode(String value) {
        this.avleverendeKomponentKode = value;
    }

    /**
     * Gets the value of the mottakendeKomponentKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMottakendeKomponentKode() {
        return mottakendeKomponentKode;
    }

    /**
     * Sets the value of the mottakendeKomponentKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMottakendeKomponentKode(String value) {
        this.mottakendeKomponentKode = value;
    }

    /**
     * Gets the value of the underkomponentKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnderkomponentKode() {
        return underkomponentKode;
    }

    /**
     * Sets the value of the underkomponentKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnderkomponentKode(String value) {
        this.underkomponentKode = value;
    }

    /**
     * Gets the value of the nokkelFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokkelFom() {
        return nokkelFom;
    }

    /**
     * Sets the value of the nokkelFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokkelFom(String value) {
        this.nokkelFom = value;
    }

    /**
     * Gets the value of the nokkelTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokkelTom() {
        return nokkelTom;
    }

    /**
     * Sets the value of the nokkelTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokkelTom(String value) {
        this.nokkelTom = value;
    }

    /**
     * Gets the value of the tidspunktAvstemmingTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTidspunktAvstemmingTom() {
        return tidspunktAvstemmingTom;
    }

    /**
     * Sets the value of the tidspunktAvstemmingTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTidspunktAvstemmingTom(String value) {
        this.tidspunktAvstemmingTom = value;
    }

    /**
     * Gets the value of the avleverendeAvstemmingId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvleverendeAvstemmingId() {
        return avleverendeAvstemmingId;
    }

    /**
     * Sets the value of the avleverendeAvstemmingId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvleverendeAvstemmingId(String value) {
        this.avleverendeAvstemmingId = value;
    }

    /**
     * Gets the value of the brukerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBrukerId() {
        return brukerId;
    }

    /**
     * Sets the value of the brukerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBrukerId(String value) {
        this.brukerId = value;
    }

}
