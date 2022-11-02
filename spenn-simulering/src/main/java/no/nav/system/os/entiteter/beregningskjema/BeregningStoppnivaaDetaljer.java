
package no.nav.system.os.entiteter.beregningskjema;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
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
 *         &lt;element name="faktiskFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="faktiskTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="kontoStreng">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="0"/>
 *               &lt;maxLength value="19"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="behandlingskode" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}string1"/>
 *         &lt;element name="belop" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}belop"/>
 *         &lt;element name="trekkVedtakId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}trekkVedtakId"/>
 *         &lt;element name="stonadId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}stonadId"/>
 *         &lt;element name="korrigering" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}string1"/>
 *         &lt;element name="tilbakeforing" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="linjeId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}linjeId"/>
 *         &lt;element name="sats" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}sats"/>
 *         &lt;element name="typeSats" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}typeSats"/>
 *         &lt;element name="antallSats" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}antallSats"/>
 *         &lt;element name="saksbehId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}saksbehId"/>
 *         &lt;element name="uforeGrad" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}grad"/>
 *         &lt;element name="kravhaverId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
 *         &lt;element name="delytelseId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}delytelseId"/>
 *         &lt;element name="bostedsenhet" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}enhet"/>
 *         &lt;element name="skykldnerId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
 *         &lt;element name="klassekode" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeKlasse"/>
 *         &lt;element name="klasseKodeBeskrivelse" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeKlasseBeskrivelse"/>
 *         &lt;element name="typeKlasse" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}typeKlasse"/>
 *         &lt;element name="typeKlasseBeskrivelse" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}typeKlasseBeskrivelse"/>
 *         &lt;element name="refunderesOrgNr" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
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
    "faktiskFom",
    "faktiskTom",
    "kontoStreng",
    "behandlingskode",
    "belop",
    "trekkVedtakId",
    "stonadId",
    "korrigering",
    "tilbakeforing",
    "linjeId",
    "sats",
    "typeSats",
    "antallSats",
    "saksbehId",
    "uforeGrad",
    "kravhaverId",
    "delytelseId",
    "bostedsenhet",
    "skykldnerId",
    "klassekode",
    "klasseKodeBeskrivelse",
    "typeKlasse",
    "typeKlasseBeskrivelse",
    "refunderesOrgNr"
})
@XmlRootElement(name = "beregningStoppnivaaDetaljer")
public class BeregningStoppnivaaDetaljer {

    @XmlElement(required = true)
    protected String faktiskFom;
    @XmlElement(required = true)
    protected String faktiskTom;
    @XmlElement(required = true)
    protected String kontoStreng;
    @XmlElement(required = true)
    protected String behandlingskode;
    @XmlElement(required = true)
    protected BigDecimal belop;
    protected long trekkVedtakId;
    @XmlElement(required = true)
    protected String stonadId;
    @XmlElement(required = true)
    protected String korrigering;
    protected boolean tilbakeforing;
    @XmlElement(required = true)
    protected BigInteger linjeId;
    @XmlElement(required = true)
    protected BigDecimal sats;
    @XmlElement(required = true)
    protected String typeSats;
    @XmlElement(required = true)
    protected BigDecimal antallSats;
    @XmlElement(required = true)
    protected String saksbehId;
    @XmlElement(required = true)
    protected BigInteger uforeGrad;
    @XmlElement(required = true)
    protected String kravhaverId;
    @XmlElement(required = true)
    protected String delytelseId;
    @XmlElement(required = true)
    protected String bostedsenhet;
    @XmlElement(required = true)
    protected String skykldnerId;
    @XmlElement(required = true)
    protected String klassekode;
    @XmlElement(required = true)
    protected String klasseKodeBeskrivelse;
    @XmlElement(required = true)
    protected String typeKlasse;
    @XmlElement(required = true)
    protected String typeKlasseBeskrivelse;
    @XmlElement(required = true)
    protected String refunderesOrgNr;

    /**
     * Gets the value of the faktiskFom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaktiskFom() {
        return faktiskFom;
    }

    /**
     * Sets the value of the faktiskFom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaktiskFom(String value) {
        this.faktiskFom = value;
    }

    /**
     * Gets the value of the faktiskTom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFaktiskTom() {
        return faktiskTom;
    }

    /**
     * Sets the value of the faktiskTom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFaktiskTom(String value) {
        this.faktiskTom = value;
    }

    /**
     * Gets the value of the kontoStreng property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKontoStreng() {
        return kontoStreng;
    }

    /**
     * Sets the value of the kontoStreng property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKontoStreng(String value) {
        this.kontoStreng = value;
    }

    /**
     * Gets the value of the behandlingskode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBehandlingskode() {
        return behandlingskode;
    }

    /**
     * Sets the value of the behandlingskode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBehandlingskode(String value) {
        this.behandlingskode = value;
    }

    /**
     * Gets the value of the belop property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getBelop() {
        return belop;
    }

    /**
     * Sets the value of the belop property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setBelop(BigDecimal value) {
        this.belop = value;
    }

    /**
     * Gets the value of the trekkVedtakId property.
     * 
     */
    public long getTrekkVedtakId() {
        return trekkVedtakId;
    }

    /**
     * Sets the value of the trekkVedtakId property.
     * 
     */
    public void setTrekkVedtakId(long value) {
        this.trekkVedtakId = value;
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
     * Gets the value of the tilbakeforing property.
     * 
     */
    public boolean isTilbakeforing() {
        return tilbakeforing;
    }

    /**
     * Sets the value of the tilbakeforing property.
     * 
     */
    public void setTilbakeforing(boolean value) {
        this.tilbakeforing = value;
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
     * Gets the value of the antallSats property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getAntallSats() {
        return antallSats;
    }

    /**
     * Sets the value of the antallSats property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setAntallSats(BigDecimal value) {
        this.antallSats = value;
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
     * Gets the value of the uforeGrad property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getUforeGrad() {
        return uforeGrad;
    }

    /**
     * Sets the value of the uforeGrad property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setUforeGrad(BigInteger value) {
        this.uforeGrad = value;
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
     * Gets the value of the skykldnerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSkykldnerId() {
        return skykldnerId;
    }

    /**
     * Sets the value of the skykldnerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSkykldnerId(String value) {
        this.skykldnerId = value;
    }

    /**
     * Gets the value of the klassekode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKlassekode() {
        return klassekode;
    }

    /**
     * Sets the value of the klassekode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKlassekode(String value) {
        this.klassekode = value;
    }

    /**
     * Gets the value of the klasseKodeBeskrivelse property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKlasseKodeBeskrivelse() {
        return klasseKodeBeskrivelse;
    }

    /**
     * Sets the value of the klasseKodeBeskrivelse property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKlasseKodeBeskrivelse(String value) {
        this.klasseKodeBeskrivelse = value;
    }

    /**
     * Gets the value of the typeKlasse property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeKlasse() {
        return typeKlasse;
    }

    /**
     * Sets the value of the typeKlasse property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeKlasse(String value) {
        this.typeKlasse = value;
    }

    /**
     * Gets the value of the typeKlasseBeskrivelse property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeKlasseBeskrivelse() {
        return typeKlasseBeskrivelse;
    }

    /**
     * Sets the value of the typeKlasseBeskrivelse property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeKlasseBeskrivelse(String value) {
        this.typeKlasseBeskrivelse = value;
    }

    /**
     * Gets the value of the refunderesOrgNr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefunderesOrgNr() {
        return refunderesOrgNr;
    }

    /**
     * Sets the value of the refunderesOrgNr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefunderesOrgNr(String value) {
        this.refunderesOrgNr = value;
    }

}
