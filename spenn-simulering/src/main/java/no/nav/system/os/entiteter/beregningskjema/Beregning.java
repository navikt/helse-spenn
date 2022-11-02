
package no.nav.system.os.entiteter.beregningskjema;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * Referanse ID 311
 * 
 * <p>Java class for beregning complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="beregning">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="gjelderId" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}fnrOrgnr"/>
 *         &lt;element name="gjelderNavn" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}navn"/>
 *         &lt;element name="datoBeregnet" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato"/>
 *         &lt;element name="kodeFaggruppe" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}kodeFaggruppe"/>
 *         &lt;element name="belop" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}belop"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/beregningSkjema}beregningsPeriode" maxOccurs="999"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "beregning", propOrder = {
    "gjelderId",
    "gjelderNavn",
    "datoBeregnet",
    "kodeFaggruppe",
    "belop",
    "beregningsPeriode"
})
public class Beregning {

    @XmlElement(required = true)
    protected String gjelderId;
    @XmlElement(required = true)
    protected String gjelderNavn;
    @XmlElement(required = true)
    protected String datoBeregnet;
    @XmlElement(required = true)
    protected String kodeFaggruppe;
    @XmlElement(required = true)
    protected BigDecimal belop;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/beregningSkjema", required = true)
    protected List<BeregningsPeriode> beregningsPeriode;

    /**
     * Gets the value of the gjelderId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGjelderId() {
        return gjelderId;
    }

    /**
     * Sets the value of the gjelderId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGjelderId(String value) {
        this.gjelderId = value;
    }

    /**
     * Gets the value of the gjelderNavn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGjelderNavn() {
        return gjelderNavn;
    }

    /**
     * Sets the value of the gjelderNavn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGjelderNavn(String value) {
        this.gjelderNavn = value;
    }

    /**
     * Gets the value of the datoBeregnet property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatoBeregnet() {
        return datoBeregnet;
    }

    /**
     * Sets the value of the datoBeregnet property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatoBeregnet(String value) {
        this.datoBeregnet = value;
    }

    /**
     * Gets the value of the kodeFaggruppe property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKodeFaggruppe() {
        return kodeFaggruppe;
    }

    /**
     * Sets the value of the kodeFaggruppe property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKodeFaggruppe(String value) {
        this.kodeFaggruppe = value;
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
     * Gets the value of the beregningsPeriode property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the beregningsPeriode property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBeregningsPeriode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BeregningsPeriode }
     * 
     * 
     */
    public List<BeregningsPeriode> getBeregningsPeriode() {
        if (beregningsPeriode == null) {
            beregningsPeriode = new ArrayList<BeregningsPeriode>();
        }
        return this.beregningsPeriode;
    }

}
