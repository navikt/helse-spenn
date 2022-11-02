
package no.nav.virksomhet.tjenester.avstemming.meldinger.v1;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;


/**
 * Grensesnittavstemmingen skal minimum bestå av en id-110 (aksjonskode ’DATA) og en totalrecord (id-120) i tillegg til START- og SLUTT-recorden.
 * 
 * <p>Java class for Totaldata complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Totaldata">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="totalAntall" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="totalBelop" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/>
 *         &lt;element name="fortegn" type="{http://nav.no/virksomhet/tjenester/avstemming/meldinger/v1}Fortegn" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Totaldata", propOrder = {
    "totalAntall",
    "totalBelop",
    "fortegn"
})
public class Totaldata {

    protected int totalAntall;
    protected BigDecimal totalBelop;
    protected Fortegn fortegn;

    /**
     * Gets the value of the totalAntall property.
     * 
     */
    public int getTotalAntall() {
        return totalAntall;
    }

    /**
     * Sets the value of the totalAntall property.
     * 
     */
    public void setTotalAntall(int value) {
        this.totalAntall = value;
    }

    /**
     * Gets the value of the totalBelop property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTotalBelop() {
        return totalBelop;
    }

    /**
     * Sets the value of the totalBelop property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTotalBelop(BigDecimal value) {
        this.totalBelop = value;
    }

    /**
     * Gets the value of the fortegn property.
     * 
     * @return
     *     possible object is
     *     {@link Fortegn }
     *     
     */
    public Fortegn getFortegn() {
        return fortegn;
    }

    /**
     * Sets the value of the fortegn property.
     * 
     * @param value
     *     allowed object is
     *     {@link Fortegn }
     *     
     */
    public void setFortegn(Fortegn value) {
        this.fortegn = value;
    }

}
