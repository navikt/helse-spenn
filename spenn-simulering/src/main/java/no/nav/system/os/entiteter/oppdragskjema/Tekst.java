
package no.nav.system.os.entiteter.oppdragskjema;

import jakarta.xml.bind.annotation.*;
import java.math.BigInteger;


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
 *         &lt;element name="tekstLnr" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}tekstLnr"/>
 *         &lt;element name="tekstKode" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="4"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="tekst" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="40"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="datoTekstFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="datoTekstTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="feilreg" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}feilreg" minOccurs="0"/>
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
    "tekstLnr",
    "tekstKode",
    "tekst",
    "datoTekstFom",
    "datoTekstTom",
    "feilreg"
})
@XmlRootElement(name = "tekst")
public class Tekst {

    @XmlElement(required = true)
    protected BigInteger tekstLnr;
    protected String tekstKode;
    protected String tekst;
    @XmlElement(required = true)
    protected String datoTekstFom;
    protected String datoTekstTom;
    protected String feilreg;

    /**
     * Gets the value of the tekstLnr property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTekstLnr() {
        return tekstLnr;
    }

    /**
     * Sets the value of the tekstLnr property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTekstLnr(BigInteger value) {
        this.tekstLnr = value;
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
     * Gets the value of the datoTekstFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoTekstFom() {
        return datoTekstFom;
    }

    /**
     * Sets the value of the datoTekstFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoTekstFom(String value) {
        this.datoTekstFom = value;
    }

    /**
     * Gets the value of the datoTekstTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoTekstTom() {
        return datoTekstTom;
    }

    /**
     * Sets the value of the datoTekstTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoTekstTom(String value) {
        this.datoTekstTom = value;
    }

    /**
     * Gets the value of the feilreg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFeilreg() {
        return feilreg;
    }

    /**
     * Sets the value of the feilreg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFeilreg(String value) {
        this.feilreg = value;
    }

}
