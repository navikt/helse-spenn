
package no.nav.system.os.entiteter.oppdragskjema;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;


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
 *         &lt;element name="typeGrense">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="4"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="belopGrense" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}belop"/>
 *         &lt;element name="datoGrenseFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="datoGrenseTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
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
    "typeGrense",
    "belopGrense",
    "datoGrenseFom",
    "datoGrenseTom",
    "feilreg"
})
@XmlRootElement(name = "belopsgrense")
public class Belopsgrense {

    @XmlElement(required = true)
    protected String typeGrense;
    @XmlElement(required = true)
    protected BigDecimal belopGrense;
    @XmlElement(required = true)
    protected String datoGrenseFom;
    protected String datoGrenseTom;
    protected String feilreg;

    /**
     * Gets the value of the typeGrense property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeGrense() {
        return typeGrense;
    }

    /**
     * Sets the value of the typeGrense property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeGrense(String value) {
        this.typeGrense = value;
    }

    /**
     * Gets the value of the belopGrense property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getBelopGrense() {
        return belopGrense;
    }

    /**
     * Sets the value of the belopGrense property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setBelopGrense(BigDecimal value) {
        this.belopGrense = value;
    }

    /**
     * Gets the value of the datoGrenseFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoGrenseFom() {
        return datoGrenseFom;
    }

    /**
     * Sets the value of the datoGrenseFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoGrenseFom(String value) {
        this.datoGrenseFom = value;
    }

    /**
     * Gets the value of the datoGrenseTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoGrenseTom() {
        return datoGrenseTom;
    }

    /**
     * Sets the value of the datoGrenseTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoGrenseTom(String value) {
        this.datoGrenseTom = value;
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
