
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for Oppdragsdata complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Oppdragsdata">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fagomradeKode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="fagsystemId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="oppdragId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="utbetalingsfrekvens" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="forfallsdato" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stonadId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="oppdragGjelderId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="oppdragGjelderFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="saksbehandlerId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="oppdragsenhetListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Enhet" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="belopsgrenseListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Belopsgrense" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tekstListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Tekst" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="oppdragslinjeListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Oppdragslinje" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="bilagstype" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Oppdragsdata", propOrder = {
    "fagomradeKode",
    "fagsystemId",
    "oppdragId",
    "utbetalingsfrekvens",
    "forfallsdato",
    "stonadId",
    "oppdragGjelderId",
    "oppdragGjelderFom",
    "saksbehandlerId",
    "oppdragsenhetListe",
    "belopsgrenseListe",
    "tekstListe",
    "oppdragslinjeListe",
    "bilagstype"
})
public class Oppdragsdata {

    @XmlElement(required = true)
    protected String fagomradeKode;
    @XmlElement(required = true)
    protected String fagsystemId;
    protected String oppdragId;
    protected String utbetalingsfrekvens;
    protected String forfallsdato;
    protected String stonadId;
    protected String oppdragGjelderId;
    protected String oppdragGjelderFom;
    protected String saksbehandlerId;
    protected List<Enhet> oppdragsenhetListe;
    protected List<Belopsgrense> belopsgrenseListe;
    protected List<Tekst> tekstListe;
    protected List<Oppdragslinje> oppdragslinjeListe;
    protected String bilagstype;

    /**
     * Gets the value of the fagomradeKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFagomradeKode() {
        return fagomradeKode;
    }

    /**
     * Sets the value of the fagomradeKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFagomradeKode(String value) {
        this.fagomradeKode = value;
    }

    /**
     * Gets the value of the fagsystemId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFagsystemId() {
        return fagsystemId;
    }

    /**
     * Sets the value of the fagsystemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFagsystemId(String value) {
        this.fagsystemId = value;
    }

    /**
     * Gets the value of the oppdragId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOppdragId() {
        return oppdragId;
    }

    /**
     * Sets the value of the oppdragId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOppdragId(String value) {
        this.oppdragId = value;
    }

    /**
     * Gets the value of the utbetalingsfrekvens property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtbetalingsfrekvens() {
        return utbetalingsfrekvens;
    }

    /**
     * Sets the value of the utbetalingsfrekvens property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtbetalingsfrekvens(String value) {
        this.utbetalingsfrekvens = value;
    }

    /**
     * Gets the value of the forfallsdato property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForfallsdato() {
        return forfallsdato;
    }

    /**
     * Sets the value of the forfallsdato property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForfallsdato(String value) {
        this.forfallsdato = value;
    }

    /**
     * Gets the value of the stonadId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStonadId() {
        return stonadId;
    }

    /**
     * Sets the value of the stonadId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStonadId(String value) {
        this.stonadId = value;
    }

    /**
     * Gets the value of the oppdragGjelderId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOppdragGjelderId() {
        return oppdragGjelderId;
    }

    /**
     * Sets the value of the oppdragGjelderId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOppdragGjelderId(String value) {
        this.oppdragGjelderId = value;
    }

    /**
     * Gets the value of the oppdragGjelderFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOppdragGjelderFom() {
        return oppdragGjelderFom;
    }

    /**
     * Sets the value of the oppdragGjelderFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOppdragGjelderFom(String value) {
        this.oppdragGjelderFom = value;
    }

    /**
     * Gets the value of the saksbehandlerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSaksbehandlerId() {
        return saksbehandlerId;
    }

    /**
     * Sets the value of the saksbehandlerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSaksbehandlerId(String value) {
        this.saksbehandlerId = value;
    }

    /**
     * Gets the value of the oppdragsenhetListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the oppdragsenhetListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOppdragsenhetListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Enhet }
     * 
     * 
     */
    public List<Enhet> getOppdragsenhetListe() {
        if (oppdragsenhetListe == null) {
            oppdragsenhetListe = new ArrayList<Enhet>();
        }
        return this.oppdragsenhetListe;
    }

    /**
     * Gets the value of the belopsgrenseListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the belopsgrenseListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBelopsgrenseListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Belopsgrense }
     * 
     * 
     */
    public List<Belopsgrense> getBelopsgrenseListe() {
        if (belopsgrenseListe == null) {
            belopsgrenseListe = new ArrayList<Belopsgrense>();
        }
        return this.belopsgrenseListe;
    }

    /**
     * Gets the value of the tekstListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tekstListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTekstListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tekst }
     * 
     * 
     */
    public List<Tekst> getTekstListe() {
        if (tekstListe == null) {
            tekstListe = new ArrayList<Tekst>();
        }
        return this.tekstListe;
    }

    /**
     * Gets the value of the oppdragslinjeListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the oppdragslinjeListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOppdragslinjeListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Oppdragslinje }
     * 
     * 
     */
    public List<Oppdragslinje> getOppdragslinjeListe() {
        if (oppdragslinjeListe == null) {
            oppdragslinjeListe = new ArrayList<Oppdragslinje>();
        }
        return this.oppdragslinjeListe;
    }

    /**
     * Gets the value of the bilagstype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBilagstype() {
        return bilagstype;
    }

    /**
     * Sets the value of the bilagstype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBilagstype(String value) {
        this.bilagstype = value;
    }

}
