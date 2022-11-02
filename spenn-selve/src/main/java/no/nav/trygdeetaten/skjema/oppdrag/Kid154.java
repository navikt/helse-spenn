//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.02.26 at 08:29:59 AM UTC 
//


package no.nav.trygdeetaten.skjema.oppdrag;

import jakarta.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Inneholder elementene som skal være med i en output 154-rekord, KID
 * 
 * <p>Java class for kid-154 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="kid-154"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="kid" type="{http://www.trygdeetaten.no/skjema/oppdrag}Tkid"/&gt;
 *         &lt;element name="datoKidFom" type="{http://www.trygdeetaten.no/skjema/oppdrag}Tdato"/&gt;
 *         &lt;element name="tidspktReg" type="{http://www.trygdeetaten.no/skjema/oppdrag}TtidspktReg"/&gt;
 *         &lt;element name="saksbehId" type="{http://www.trygdeetaten.no/skjema/oppdrag}TsaksbehId"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "kid-154", propOrder = {
    "kid",
    "datoKidFom",
    "tidspktReg",
    "saksbehId"
})
public class Kid154 {

    @XmlElement(required = true)
    protected String kid;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar datoKidFom;
    @XmlElement(required = true)
    protected String tidspktReg;
    @XmlElement(required = true)
    protected String saksbehId;

    /**
     * Gets the value of the kid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKid() {
        return kid;
    }

    /**
     * Sets the value of the kid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKid(String value) {
        this.kid = value;
    }

    /**
     * Gets the value of the datoKidFom property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDatoKidFom() {
        return datoKidFom;
    }

    /**
     * Sets the value of the datoKidFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDatoKidFom(XMLGregorianCalendar value) {
        this.datoKidFom = value;
    }

    /**
     * Gets the value of the tidspktReg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTidspktReg() {
        return tidspktReg;
    }

    /**
     * Sets the value of the tidspktReg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTidspktReg(String value) {
        this.tidspktReg = value;
    }

    /**
     * Gets the value of the saksbehId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSaksbehId() {
        return saksbehId;
    }

    /**
     * Sets the value of the saksbehId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSaksbehId(String value) {
        this.saksbehId = value;
    }

    public Kid154 withKid(String value) {
        setKid(value);
        return this;
    }

    public Kid154 withDatoKidFom(XMLGregorianCalendar value) {
        setDatoKidFom(value);
        return this;
    }

    public Kid154 withTidspktReg(String value) {
        setTidspktReg(value);
        return this;
    }

    public Kid154 withSaksbehId(String value) {
        setSaksbehId(value);
        return this;
    }

}
