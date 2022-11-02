
package no.nav.system.os.entiteter.oppdragskjema;

import no.nav.system.os.entiteter.typer.simpletypes.KodeStatus;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Referanse ID 110
 * 
 * <p>Java class for oppdrag complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="oppdrag">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="kodeEndring">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="NY"/>
 *               &lt;enumeration value="ENDR"/>
 *               &lt;enumeration value="UEND"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="kodeStatus" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeStatus" minOccurs="0"/>
 *         &lt;element name="datoStatusFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="kodeFagomraade" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeFagomraade"/>
 *         &lt;element name="fagsystemId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fagsystemId" minOccurs="0"/>
 *         &lt;element name="oppdragsId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}oppdragsId" minOccurs="0"/>
 *         &lt;element name="utbetFrekvens" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}utbetFrekvens" minOccurs="0"/>
 *         &lt;element name="datoForfall" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="stonadId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}stonadId" minOccurs="0"/>
 *         &lt;element name="oppdragGjelderId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
 *         &lt;element name="datoOppdragGjelderFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="saksbehId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}saksbehId"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}enhet" maxOccurs="2" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}belopsgrense" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}tekst" maxOccurs="50" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "oppdrag2", propOrder = {
    "kodeEndring",
    "kodeStatus",
    "datoStatusFom",
    "kodeFagomraade",
    "fagsystemId",
    "oppdragsId",
    "utbetFrekvens",
    "datoForfall",
    "stonadId",
    "oppdragGjelderId",
    "datoOppdragGjelderFom",
    "saksbehId",
    "enhet",
    "belopsgrense",
    "tekst"
})
@XmlSeeAlso({
    no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag.class
})
public class Oppdrag {

    @XmlElement(required = true)
    protected String kodeEndring;
    protected KodeStatus kodeStatus;
    protected String datoStatusFom;
    @XmlElement(required = true)
    protected String kodeFagomraade;
    protected String fagsystemId;
    protected Long oppdragsId;
    protected String utbetFrekvens;
    protected String datoForfall;
    protected String stonadId;
    @XmlElement(required = true)
    protected String oppdragGjelderId;
    @XmlElement(required = true)
    protected String datoOppdragGjelderFom;
    @XmlElement(required = true)
    protected String saksbehId;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Enhet> enhet;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Belopsgrense> belopsgrense;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Tekst> tekst;

    /**
     * Gets the value of the kodeEndring property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKodeEndring() {
        return kodeEndring;
    }

    /**
     * Sets the value of the kodeEndring property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKodeEndring(String value) {
        this.kodeEndring = value;
    }

    /**
     * Gets the value of the kodeStatus property.
     * 
     * @return
     *     possible object is
     *     {@link KodeStatus }
     *     
     */
    public KodeStatus getKodeStatus() {
        return kodeStatus;
    }

    /**
     * Sets the value of the kodeStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link KodeStatus }
     *     
     */
    public void setKodeStatus(KodeStatus value) {
        this.kodeStatus = value;
    }

    /**
     * Gets the value of the datoStatusFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoStatusFom() {
        return datoStatusFom;
    }

    /**
     * Sets the value of the datoStatusFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoStatusFom(String value) {
        this.datoStatusFom = value;
    }

    /**
     * Gets the value of the kodeFagomraade property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKodeFagomraade() {
        return kodeFagomraade;
    }

    /**
     * Sets the value of the kodeFagomraade property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKodeFagomraade(String value) {
        this.kodeFagomraade = value;
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
     * Gets the value of the oppdragsId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getOppdragsId() {
        return oppdragsId;
    }

    /**
     * Sets the value of the oppdragsId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setOppdragsId(Long value) {
        this.oppdragsId = value;
    }

    /**
     * Gets the value of the utbetFrekvens property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtbetFrekvens() {
        return utbetFrekvens;
    }

    /**
     * Sets the value of the utbetFrekvens property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtbetFrekvens(String value) {
        this.utbetFrekvens = value;
    }

    /**
     * Gets the value of the datoForfall property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoForfall() {
        return datoForfall;
    }

    /**
     * Sets the value of the datoForfall property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoForfall(String value) {
        this.datoForfall = value;
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
     * Gets the value of the datoOppdragGjelderFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoOppdragGjelderFom() {
        return datoOppdragGjelderFom;
    }

    /**
     * Sets the value of the datoOppdragGjelderFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoOppdragGjelderFom(String value) {
        this.datoOppdragGjelderFom = value;
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

    /**
     * Gets the value of the enhet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the enhet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEnhet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Enhet }
     * 
     * 
     */
    public List<Enhet> getEnhet() {
        if (enhet == null) {
            enhet = new ArrayList<Enhet>();
        }
        return this.enhet;
    }

    /**
     * Gets the value of the belopsgrense property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the belopsgrense property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBelopsgrense().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Belopsgrense }
     * 
     * 
     */
    public List<Belopsgrense> getBelopsgrense() {
        if (belopsgrense == null) {
            belopsgrense = new ArrayList<Belopsgrense>();
        }
        return this.belopsgrense;
    }

    /**
     * Gets the value of the tekst property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tekst property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTekst().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tekst }
     * 
     * 
     */
    public List<Tekst> getTekst() {
        if (tekst == null) {
            tekst = new ArrayList<Tekst>();
        }
        return this.tekst;
    }

}
