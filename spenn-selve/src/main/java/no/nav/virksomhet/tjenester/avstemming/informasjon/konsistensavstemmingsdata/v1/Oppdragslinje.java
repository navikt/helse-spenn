
package no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for Oppdragslinje complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Oppdragslinje">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="vedtakId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="delytelseId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="linjeId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="klassifikasjonKode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="klassifkasjonFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="vedtakPeriode" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Periode" minOccurs="0"/>
 *         &lt;element name="sats" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/>
 *         &lt;element name="satstypeKode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fradragTillegg" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="skyldnerId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="skyldnerFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="kravhaverId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="kravhaverFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="kundeidentifikasjon" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="kundeidentifikasjonFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="brukKjoreplan" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="saksbehandlerId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="utbetalesTilId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="utbetalesFom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="arbeidsgiverKode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="henvisning" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="soknadsType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="refFagsystemId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="refOppdragId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="refDelytelseId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="refLinjeId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="linjetekstListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Tekst" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="linjeenhetListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Enhet" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="gradListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Grad" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="attestantListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Attestant" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="valutaListe" type="{http://nav.no/virksomhet/tjenester/avstemming/informasjon/konsistensavstemmingsdata/v1}Valuta" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Oppdragslinje", propOrder = {
    "vedtakId",
    "delytelseId",
    "linjeId",
    "klassifikasjonKode",
    "klassifkasjonFom",
    "vedtakPeriode",
    "sats",
    "satstypeKode",
    "fradragTillegg",
    "skyldnerId",
    "skyldnerFom",
    "kravhaverId",
    "kravhaverFom",
    "kundeidentifikasjon",
    "kundeidentifikasjonFom",
    "brukKjoreplan",
    "saksbehandlerId",
    "utbetalesTilId",
    "utbetalesFom",
    "arbeidsgiverKode",
    "henvisning",
    "soknadsType",
    "refFagsystemId",
    "refOppdragId",
    "refDelytelseId",
    "refLinjeId",
    "linjetekstListe",
    "linjeenhetListe",
    "gradListe",
    "attestantListe",
    "valutaListe"
})
public class Oppdragslinje {

    protected String vedtakId;
    protected String delytelseId;
    protected String linjeId;
    protected String klassifikasjonKode;
    protected String klassifkasjonFom;
    protected Periode vedtakPeriode;
    protected BigDecimal sats;
    protected String satstypeKode;
    protected String fradragTillegg;
    protected String skyldnerId;
    protected String skyldnerFom;
    protected String kravhaverId;
    protected String kravhaverFom;
    protected String kundeidentifikasjon;
    protected String kundeidentifikasjonFom;
    protected String brukKjoreplan;
    protected String saksbehandlerId;
    protected String utbetalesTilId;
    protected String utbetalesFom;
    protected String arbeidsgiverKode;
    protected String henvisning;
    protected String soknadsType;
    protected String refFagsystemId;
    protected String refOppdragId;
    protected String refDelytelseId;
    protected String refLinjeId;
    protected List<Tekst> linjetekstListe;
    protected List<Enhet> linjeenhetListe;
    protected List<Grad> gradListe;
    protected List<Attestant> attestantListe;
    protected List<Valuta> valutaListe;

    /**
     * Gets the value of the vedtakId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVedtakId() {
        return vedtakId;
    }

    /**
     * Sets the value of the vedtakId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVedtakId(String value) {
        this.vedtakId = value;
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
     * Gets the value of the linjeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLinjeId() {
        return linjeId;
    }

    /**
     * Sets the value of the linjeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinjeId(String value) {
        this.linjeId = value;
    }

    /**
     * Gets the value of the klassifikasjonKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKlassifikasjonKode() {
        return klassifikasjonKode;
    }

    /**
     * Sets the value of the klassifikasjonKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKlassifikasjonKode(String value) {
        this.klassifikasjonKode = value;
    }

    /**
     * Gets the value of the klassifkasjonFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKlassifkasjonFom() {
        return klassifkasjonFom;
    }

    /**
     * Sets the value of the klassifkasjonFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKlassifkasjonFom(String value) {
        this.klassifkasjonFom = value;
    }

    /**
     * Gets the value of the vedtakPeriode property.
     * 
     * @return
     *     possible object is
     *     {@link Periode }
     *     
     */
    public Periode getVedtakPeriode() {
        return vedtakPeriode;
    }

    /**
     * Sets the value of the vedtakPeriode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Periode }
     *     
     */
    public void setVedtakPeriode(Periode value) {
        this.vedtakPeriode = value;
    }

    /**
     * Gets the value of the sats property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSats() {
        return sats;
    }

    /**
     * Sets the value of the sats property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSats(BigDecimal value) {
        this.sats = value;
    }

    /**
     * Gets the value of the satstypeKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSatstypeKode() {
        return satstypeKode;
    }

    /**
     * Sets the value of the satstypeKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSatstypeKode(String value) {
        this.satstypeKode = value;
    }

    /**
     * Gets the value of the fradragTillegg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFradragTillegg() {
        return fradragTillegg;
    }

    /**
     * Sets the value of the fradragTillegg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFradragTillegg(String value) {
        this.fradragTillegg = value;
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
     * Gets the value of the skyldnerFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSkyldnerFom() {
        return skyldnerFom;
    }

    /**
     * Sets the value of the skyldnerFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSkyldnerFom(String value) {
        this.skyldnerFom = value;
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
     * Gets the value of the kravhaverFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKravhaverFom() {
        return kravhaverFom;
    }

    /**
     * Sets the value of the kravhaverFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKravhaverFom(String value) {
        this.kravhaverFom = value;
    }

    /**
     * Gets the value of the kundeidentifikasjon property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKundeidentifikasjon() {
        return kundeidentifikasjon;
    }

    /**
     * Sets the value of the kundeidentifikasjon property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKundeidentifikasjon(String value) {
        this.kundeidentifikasjon = value;
    }

    /**
     * Gets the value of the kundeidentifikasjonFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKundeidentifikasjonFom() {
        return kundeidentifikasjonFom;
    }

    /**
     * Sets the value of the kundeidentifikasjonFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKundeidentifikasjonFom(String value) {
        this.kundeidentifikasjonFom = value;
    }

    /**
     * Gets the value of the brukKjoreplan property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBrukKjoreplan() {
        return brukKjoreplan;
    }

    /**
     * Sets the value of the brukKjoreplan property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBrukKjoreplan(String value) {
        this.brukKjoreplan = value;
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
     * Gets the value of the utbetalesTilId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtbetalesTilId() {
        return utbetalesTilId;
    }

    /**
     * Sets the value of the utbetalesTilId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtbetalesTilId(String value) {
        this.utbetalesTilId = value;
    }

    /**
     * Gets the value of the utbetalesFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtbetalesFom() {
        return utbetalesFom;
    }

    /**
     * Sets the value of the utbetalesFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtbetalesFom(String value) {
        this.utbetalesFom = value;
    }

    /**
     * Gets the value of the arbeidsgiverKode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArbeidsgiverKode() {
        return arbeidsgiverKode;
    }

    /**
     * Sets the value of the arbeidsgiverKode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArbeidsgiverKode(String value) {
        this.arbeidsgiverKode = value;
    }

    /**
     * Gets the value of the henvisning property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHenvisning() {
        return henvisning;
    }

    /**
     * Sets the value of the henvisning property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHenvisning(String value) {
        this.henvisning = value;
    }

    /**
     * Gets the value of the soknadsType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSoknadsType() {
        return soknadsType;
    }

    /**
     * Sets the value of the soknadsType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSoknadsType(String value) {
        this.soknadsType = value;
    }

    /**
     * Gets the value of the refFagsystemId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefFagsystemId() {
        return refFagsystemId;
    }

    /**
     * Sets the value of the refFagsystemId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefFagsystemId(String value) {
        this.refFagsystemId = value;
    }

    /**
     * Gets the value of the refOppdragId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefOppdragId() {
        return refOppdragId;
    }

    /**
     * Sets the value of the refOppdragId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefOppdragId(String value) {
        this.refOppdragId = value;
    }

    /**
     * Gets the value of the refDelytelseId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefDelytelseId() {
        return refDelytelseId;
    }

    /**
     * Sets the value of the refDelytelseId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefDelytelseId(String value) {
        this.refDelytelseId = value;
    }

    /**
     * Gets the value of the refLinjeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefLinjeId() {
        return refLinjeId;
    }

    /**
     * Sets the value of the refLinjeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefLinjeId(String value) {
        this.refLinjeId = value;
    }

    /**
     * Gets the value of the linjetekstListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linjetekstListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinjetekstListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tekst }
     * 
     * 
     */
    public List<Tekst> getLinjetekstListe() {
        if (linjetekstListe == null) {
            linjetekstListe = new ArrayList<Tekst>();
        }
        return this.linjetekstListe;
    }

    /**
     * Gets the value of the linjeenhetListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linjeenhetListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinjeenhetListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Enhet }
     * 
     * 
     */
    public List<Enhet> getLinjeenhetListe() {
        if (linjeenhetListe == null) {
            linjeenhetListe = new ArrayList<Enhet>();
        }
        return this.linjeenhetListe;
    }

    /**
     * Gets the value of the gradListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the gradListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGradListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Grad }
     * 
     * 
     */
    public List<Grad> getGradListe() {
        if (gradListe == null) {
            gradListe = new ArrayList<Grad>();
        }
        return this.gradListe;
    }

    /**
     * Gets the value of the attestantListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attestantListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttestantListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Attestant }
     * 
     * 
     */
    public List<Attestant> getAttestantListe() {
        if (attestantListe == null) {
            attestantListe = new ArrayList<Attestant>();
        }
        return this.attestantListe;
    }

    /**
     * Gets the value of the valutaListe property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the valutaListe property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValutaListe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Valuta }
     * 
     * 
     */
    public List<Valuta> getValutaListe() {
        if (valutaListe == null) {
            valutaListe = new ArrayList<Valuta>();
        }
        return this.valutaListe;
    }

}
