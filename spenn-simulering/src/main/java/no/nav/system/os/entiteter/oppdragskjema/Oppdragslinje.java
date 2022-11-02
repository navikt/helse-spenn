
package no.nav.system.os.entiteter.oppdragskjema;

import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg;
import no.nav.system.os.entiteter.typer.simpletypes.KodeArbeidsgiver;
import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * Referanse ID 150
 * 
 * <p>Java class for oppdragslinje complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="oppdragslinje">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="kodeEndringLinje">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="NY"/>
 *               &lt;enumeration value="ENDR"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="kodeStatusLinje" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeStatusLinje" minOccurs="0"/>
 *         &lt;element name="datoStatusFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="vedtakId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}vedtakId" minOccurs="0"/>
 *         &lt;element name="delytelseId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}delytelseId" minOccurs="0"/>
 *         &lt;element name="linjeId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}linjeId" minOccurs="0"/>
 *         &lt;element name="kodeKlassifik" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeKlassifik"/>
 *         &lt;element name="datoKlassifikFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="datoVedtakFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="datoVedtakTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="sats" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}sats"/>
 *         &lt;element name="fradragTillegg" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fradragTillegg"/>
 *         &lt;element name="typeSats" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}typeSats"/>
 *         &lt;element name="skyldnerId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr" minOccurs="0"/>
 *         &lt;element name="datoSkyldnerFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="kravhaverId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr" minOccurs="0"/>
 *         &lt;element name="datoKravhaverFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="kid" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kid" minOccurs="0"/>
 *         &lt;element name="datoKidFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="brukKjoreplan" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}brukKjoreplan" minOccurs="0"/>
 *         &lt;element name="saksbehId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}saksbehId"/>
 *         &lt;element name="utbetalesTilId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
 *         &lt;element name="datoUtbetalesTilIdFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *         &lt;element name="kodeArbeidsgiver" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeArbeidsgiver" minOccurs="0"/>
 *         &lt;element name="henvisning" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}henvisning" minOccurs="0"/>
 *         &lt;element name="typeSoknad" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="10"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="refFagsystemId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fagsystemId" minOccurs="0"/>
 *         &lt;element name="refOppdragsId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}oppdragsId" minOccurs="0"/>
 *         &lt;element name="refDelytelseId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}delytelseId" minOccurs="0"/>
 *         &lt;element name="refLinjeId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}linjeId" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "oppdragslinje2", propOrder = {
    "kodeEndringLinje",
    "kodeStatusLinje",
    "datoStatusFom",
    "vedtakId",
    "delytelseId",
    "linjeId",
    "kodeKlassifik",
    "datoKlassifikFom",
    "datoVedtakFom",
    "datoVedtakTom",
    "sats",
    "fradragTillegg",
    "typeSats",
    "skyldnerId",
    "datoSkyldnerFom",
    "kravhaverId",
    "datoKravhaverFom",
    "kid",
    "datoKidFom",
    "brukKjoreplan",
    "saksbehId",
    "utbetalesTilId",
    "datoUtbetalesTilIdFom",
    "kodeArbeidsgiver",
    "henvisning",
    "typeSoknad",
    "refFagsystemId",
    "refOppdragsId",
    "refDelytelseId",
    "refLinjeId"
})
@XmlSeeAlso({
    no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje.class
})
public class Oppdragslinje {

    @XmlElement(required = true)
    protected String kodeEndringLinje;
    protected KodeStatusLinje kodeStatusLinje;
    protected String datoStatusFom;
    protected String vedtakId;
    protected String delytelseId;
    protected BigInteger linjeId;
    @XmlElement(required = true)
    protected String kodeKlassifik;
    protected String datoKlassifikFom;
    @XmlElement(required = true)
    protected String datoVedtakFom;
    protected String datoVedtakTom;
    @XmlElement(required = true)
    protected BigDecimal sats;
    @XmlElement(required = true)
    protected FradragTillegg fradragTillegg;
    @XmlElement(required = true)
    protected String typeSats;
    protected String skyldnerId;
    protected String datoSkyldnerFom;
    protected String kravhaverId;
    protected String datoKravhaverFom;
    protected String kid;
    protected String datoKidFom;
    protected String brukKjoreplan;
    @XmlElement(required = true)
    protected String saksbehId;
    @XmlElement(required = true)
    protected String utbetalesTilId;
    protected String datoUtbetalesTilIdFom;
    protected KodeArbeidsgiver kodeArbeidsgiver;
    protected String henvisning;
    protected String typeSoknad;
    protected String refFagsystemId;
    protected Long refOppdragsId;
    protected String refDelytelseId;
    protected BigInteger refLinjeId;

    /**
     * Gets the value of the kodeEndringLinje property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKodeEndringLinje() {
        return kodeEndringLinje;
    }

    /**
     * Sets the value of the kodeEndringLinje property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKodeEndringLinje(String value) {
        this.kodeEndringLinje = value;
    }

    /**
     * Gets the value of the kodeStatusLinje property.
     * 
     * @return
     *     possible object is
     *     {@link KodeStatusLinje }
     *     
     */
    public KodeStatusLinje getKodeStatusLinje() {
        return kodeStatusLinje;
    }

    /**
     * Sets the value of the kodeStatusLinje property.
     * 
     * @param value
     *     allowed object is
     *     {@link KodeStatusLinje }
     *     
     */
    public void setKodeStatusLinje(KodeStatusLinje value) {
        this.kodeStatusLinje = value;
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
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLinjeId() {
        return linjeId;
    }

    /**
     * Sets the value of the linjeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLinjeId(BigInteger value) {
        this.linjeId = value;
    }

    /**
     * Gets the value of the kodeKlassifik property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    /**
     * Sets the value of the kodeKlassifik property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKodeKlassifik(String value) {
        this.kodeKlassifik = value;
    }

    /**
     * Gets the value of the datoKlassifikFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoKlassifikFom() {
        return datoKlassifikFom;
    }

    /**
     * Sets the value of the datoKlassifikFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoKlassifikFom(String value) {
        this.datoKlassifikFom = value;
    }

    /**
     * Gets the value of the datoVedtakFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoVedtakFom() {
        return datoVedtakFom;
    }

    /**
     * Sets the value of the datoVedtakFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoVedtakFom(String value) {
        this.datoVedtakFom = value;
    }

    /**
     * Gets the value of the datoVedtakTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoVedtakTom() {
        return datoVedtakTom;
    }

    /**
     * Sets the value of the datoVedtakTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoVedtakTom(String value) {
        this.datoVedtakTom = value;
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
     * Gets the value of the fradragTillegg property.
     * 
     * @return
     *     possible object is
     *     {@link FradragTillegg }
     *     
     */
    public FradragTillegg getFradragTillegg() {
        return fradragTillegg;
    }

    /**
     * Sets the value of the fradragTillegg property.
     * 
     * @param value
     *     allowed object is
     *     {@link FradragTillegg }
     *     
     */
    public void setFradragTillegg(FradragTillegg value) {
        this.fradragTillegg = value;
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
     * Gets the value of the datoSkyldnerFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoSkyldnerFom() {
        return datoSkyldnerFom;
    }

    /**
     * Sets the value of the datoSkyldnerFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoSkyldnerFom(String value) {
        this.datoSkyldnerFom = value;
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
     * Gets the value of the datoKravhaverFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoKravhaverFom() {
        return datoKravhaverFom;
    }

    /**
     * Sets the value of the datoKravhaverFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoKravhaverFom(String value) {
        this.datoKravhaverFom = value;
    }

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
     *     {@link String }
     *     
     */
    public String getDatoKidFom() {
        return datoKidFom;
    }

    /**
     * Sets the value of the datoKidFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoKidFom(String value) {
        this.datoKidFom = value;
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
     * Gets the value of the datoUtbetalesTilIdFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoUtbetalesTilIdFom() {
        return datoUtbetalesTilIdFom;
    }

    /**
     * Sets the value of the datoUtbetalesTilIdFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoUtbetalesTilIdFom(String value) {
        this.datoUtbetalesTilIdFom = value;
    }

    /**
     * Gets the value of the kodeArbeidsgiver property.
     * 
     * @return
     *     possible object is
     *     {@link KodeArbeidsgiver }
     *     
     */
    public KodeArbeidsgiver getKodeArbeidsgiver() {
        return kodeArbeidsgiver;
    }

    /**
     * Sets the value of the kodeArbeidsgiver property.
     * 
     * @param value
     *     allowed object is
     *     {@link KodeArbeidsgiver }
     *     
     */
    public void setKodeArbeidsgiver(KodeArbeidsgiver value) {
        this.kodeArbeidsgiver = value;
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
     * Gets the value of the typeSoknad property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeSoknad() {
        return typeSoknad;
    }

    /**
     * Sets the value of the typeSoknad property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeSoknad(String value) {
        this.typeSoknad = value;
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
     * Gets the value of the refOppdragsId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getRefOppdragsId() {
        return refOppdragsId;
    }

    /**
     * Sets the value of the refOppdragsId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setRefOppdragsId(Long value) {
        this.refOppdragsId = value;
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
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRefLinjeId() {
        return refLinjeId;
    }

    /**
     * Sets the value of the refLinjeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRefLinjeId(BigInteger value) {
        this.refLinjeId = value;
    }

}
