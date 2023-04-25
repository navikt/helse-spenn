package no.nav.trygdeetaten.skjema.oppdrag;

import jakarta.xml.bind.annotation.*;

import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Inneholder elementene som skal være med i en input 180-rekord, Attestasjon
 *             
 * 
 * <p>Java class for attestant-180 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="attestant-180"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="attestantId"&gt;
 *           &lt;simpleType&gt;
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *               &lt;minLength value="1"/&gt;
 *               &lt;maxLength value="8"/&gt;
 *             &lt;/restriction&gt;
 *           &lt;/simpleType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="datoUgyldigFom" type="{http://www.trygdeetaten.no/skjema/oppdrag}Tdato" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attestant-180", propOrder = {
    "attestantId",
    "datoUgyldigFom"
})
public class Attestant180 {

    @XmlElement(required = true)
    protected String attestantId;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar datoUgyldigFom;

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
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDatoUgyldigFom() {
        return datoUgyldigFom;
    }

    /**
     * Sets the value of the datoUgyldigFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDatoUgyldigFom(XMLGregorianCalendar value) {
        this.datoUgyldigFom = value;
    }

    public Attestant180 withAttestantId(String value) {
        setAttestantId(value);
        return this;
    }

    public Attestant180 withDatoUgyldigFom(XMLGregorianCalendar value) {
        setDatoUgyldigFom(value);
        return this;
    }

}
