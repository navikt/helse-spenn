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
 * Simulering, Output 314-rekord
 * 
 * <p>Java class for simuleringsLinjer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="simuleringsLinjer"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="antallSats" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="behandlingsKode" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="belop" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="bostedsenhet" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="delytelseId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="faktiskFom" type="{http://www.w3.org/2001/XMLSchema}date"/&gt;
 *         &lt;element name="faktiskTom" type="{http://www.w3.org/2001/XMLSchema}date"/&gt;
 *         &lt;element name="funksjonaerId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="kontostreng" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="korrigering" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="kravhaverId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="linjeId" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="sats" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *         &lt;element name="skyldnerId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="stonadId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="tilbakeforing" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="trekkvedtakId" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="typeSats" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="uforeGrad" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "simuleringsLinjer", propOrder = {
    "antallSats",
    "behandlingsKode",
    "belop",
    "bostedsenhet",
    "delytelseId",
    "faktiskFom",
    "faktiskTom",
    "funksjonaerId",
    "kontostreng",
    "korrigering",
    "kravhaverId",
    "linjeId",
    "sats",
    "skyldnerId",
    "stonadId",
    "tilbakeforing",
    "trekkvedtakId",
    "typeSats",
    "uforeGrad"
})
public class SimuleringsLinjer {

    protected double antallSats;
    @XmlElement(required = true)
    protected String behandlingsKode;
    protected double belop;
    @XmlElement(required = true)
    protected String bostedsenhet;
    @XmlElement(required = true)
    protected String delytelseId;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar faktiskFom;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar faktiskTom;
    @XmlElement(required = true)
    protected String funksjonaerId;
    @XmlElement(required = true)
    protected String kontostreng;
    @XmlElement(required = true)
    protected String korrigering;
    @XmlElement(required = true)
    protected String kravhaverId;
    protected int linjeId;
    protected double sats;
    @XmlElement(required = true)
    protected String skyldnerId;
    @XmlElement(required = true)
    protected String stonadId;
    @XmlElement(required = true)
    protected String tilbakeforing;
    protected long trekkvedtakId;
    @XmlElement(required = true)
    protected String typeSats;
    @XmlElement(required = true)
    protected String uforeGrad;

    /**
     * Gets the value of the antallSats property.
     * 
     */
    public double getAntallSats() {
        return antallSats;
    }

    /**
     * Sets the value of the antallSats property.
     * 
     */
    public void setAntallSats(double value) {
        this.antallSats = value;
    }

    /**
     * Gets the value of the behandlingsKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBehandlingsKode() {
        return behandlingsKode;
    }

    /**
     * Sets the value of the behandlingsKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBehandlingsKode(String value) {
        this.behandlingsKode = value;
    }

    /**
     * Gets the value of the belop property.
     * 
     */
    public double getBelop() {
        return belop;
    }

    /**
     * Sets the value of the belop property.
     * 
     */
    public void setBelop(double value) {
        this.belop = value;
    }

    /**
     * Gets the value of the bostedsenhet property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBostedsenhet() {
        return bostedsenhet;
    }

    /**
     * Sets the value of the bostedsenhet property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBostedsenhet(String value) {
        this.bostedsenhet = value;
    }

    /**
     * Gets the value of the delytelseId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDelytelseId() {
        return delytelseId;
    }

    /**
     * Sets the value of the delytelseId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDelytelseId(String value) {
        this.delytelseId = value;
    }

    /**
     * Gets the value of the faktiskFom property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFaktiskFom() {
        return faktiskFom;
    }

    /**
     * Sets the value of the faktiskFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFaktiskFom(XMLGregorianCalendar value) {
        this.faktiskFom = value;
    }

    /**
     * Gets the value of the faktiskTom property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFaktiskTom() {
        return faktiskTom;
    }

    /**
     * Sets the value of the faktiskTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFaktiskTom(XMLGregorianCalendar value) {
        this.faktiskTom = value;
    }

    /**
     * Gets the value of the funksjonaerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFunksjonaerId() {
        return funksjonaerId;
    }

    /**
     * Sets the value of the funksjonaerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFunksjonaerId(String value) {
        this.funksjonaerId = value;
    }

    /**
     * Gets the value of the kontostreng property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKontostreng() {
        return kontostreng;
    }

    /**
     * Sets the value of the kontostreng property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKontostreng(String value) {
        this.kontostreng = value;
    }

    /**
     * Gets the value of the korrigering property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKorrigering() {
        return korrigering;
    }

    /**
     * Sets the value of the korrigering property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKorrigering(String value) {
        this.korrigering = value;
    }

    /**
     * Gets the value of the kravhaverId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKravhaverId() {
        return kravhaverId;
    }

    /**
     * Sets the value of the kravhaverId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKravhaverId(String value) {
        this.kravhaverId = value;
    }

    /**
     * Gets the value of the linjeId property.
     * 
     */
    public int getLinjeId() {
        return linjeId;
    }

    /**
     * Sets the value of the linjeId property.
     * 
     */
    public void setLinjeId(int value) {
        this.linjeId = value;
    }

    /**
     * Gets the value of the sats property.
     * 
     */
    public double getSats() {
        return sats;
    }

    /**
     * Sets the value of the sats property.
     * 
     */
    public void setSats(double value) {
        this.sats = value;
    }

    /**
     * Gets the value of the skyldnerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSkyldnerId() {
        return skyldnerId;
    }

    /**
     * Sets the value of the skyldnerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSkyldnerId(String value) {
        this.skyldnerId = value;
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
     * Gets the value of the tilbakeforing property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTilbakeforing() {
        return tilbakeforing;
    }

    /**
     * Sets the value of the tilbakeforing property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTilbakeforing(String value) {
        this.tilbakeforing = value;
    }

    /**
     * Gets the value of the trekkvedtakId property.
     * 
     */
    public long getTrekkvedtakId() {
        return trekkvedtakId;
    }

    /**
     * Sets the value of the trekkvedtakId property.
     * 
     */
    public void setTrekkvedtakId(long value) {
        this.trekkvedtakId = value;
    }

    /**
     * Gets the value of the typeSats property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeSats() {
        return typeSats;
    }

    /**
     * Sets the value of the typeSats property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeSats(String value) {
        this.typeSats = value;
    }

    /**
     * Gets the value of the uforeGrad property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUforeGrad() {
        return uforeGrad;
    }

    /**
     * Sets the value of the uforeGrad property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUforeGrad(String value) {
        this.uforeGrad = value;
    }

    public SimuleringsLinjer withAntallSats(double value) {
        setAntallSats(value);
        return this;
    }

    public SimuleringsLinjer withBehandlingsKode(String value) {
        setBehandlingsKode(value);
        return this;
    }

    public SimuleringsLinjer withBelop(double value) {
        setBelop(value);
        return this;
    }

    public SimuleringsLinjer withBostedsenhet(String value) {
        setBostedsenhet(value);
        return this;
    }

    public SimuleringsLinjer withDelytelseId(String value) {
        setDelytelseId(value);
        return this;
    }

    public SimuleringsLinjer withFaktiskFom(XMLGregorianCalendar value) {
        setFaktiskFom(value);
        return this;
    }

    public SimuleringsLinjer withFaktiskTom(XMLGregorianCalendar value) {
        setFaktiskTom(value);
        return this;
    }

    public SimuleringsLinjer withFunksjonaerId(String value) {
        setFunksjonaerId(value);
        return this;
    }

    public SimuleringsLinjer withKontostreng(String value) {
        setKontostreng(value);
        return this;
    }

    public SimuleringsLinjer withKorrigering(String value) {
        setKorrigering(value);
        return this;
    }

    public SimuleringsLinjer withKravhaverId(String value) {
        setKravhaverId(value);
        return this;
    }

    public SimuleringsLinjer withLinjeId(int value) {
        setLinjeId(value);
        return this;
    }

    public SimuleringsLinjer withSats(double value) {
        setSats(value);
        return this;
    }

    public SimuleringsLinjer withSkyldnerId(String value) {
        setSkyldnerId(value);
        return this;
    }

    public SimuleringsLinjer withStonadId(String value) {
        setStonadId(value);
        return this;
    }

    public SimuleringsLinjer withTilbakeforing(String value) {
        setTilbakeforing(value);
        return this;
    }

    public SimuleringsLinjer withTrekkvedtakId(long value) {
        setTrekkvedtakId(value);
        return this;
    }

    public SimuleringsLinjer withTypeSats(String value) {
        setTypeSats(value);
        return this;
    }

    public SimuleringsLinjer withUforeGrad(String value) {
        setUforeGrad(value);
        return this;
    }

}
