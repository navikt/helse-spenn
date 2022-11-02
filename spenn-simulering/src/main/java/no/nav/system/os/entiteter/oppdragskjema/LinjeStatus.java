
package no.nav.system.os.entiteter.oppdragskjema;

import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje;

import jakarta.xml.bind.annotation.*;


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
 *         &lt;element name="kodeStatusLinje" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeStatusLinje"/>
 *         &lt;element name="datoStatusFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="tidspktReg" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}tidspktReg"/>
 *         &lt;element name="saksbehId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}saksbehId"/>
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
    "kodeStatusLinje",
    "datoStatusFom",
    "tidspktReg",
    "saksbehId"
})
@XmlRootElement(name = "linjeStatus")
public class LinjeStatus {

    @XmlElement(required = true)
    protected KodeStatusLinje kodeStatusLinje;
    @XmlElement(required = true)
    protected String datoStatusFom;
    @XmlElement(required = true)
    protected String tidspktReg;
    @XmlElement(required = true)
    protected String saksbehId;

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

}
