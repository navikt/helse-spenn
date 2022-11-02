
package no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes;

import no.nav.system.os.entiteter.beregningskjema.Beregning;
import no.nav.system.os.entiteter.infomelding.Infomelding;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for simulerBeregningResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="simulerBeregningResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="simulering" type="{http://nav.no/system/os/entiteter/beregningSkjema}beregning" minOccurs="0"/>
 *         &lt;element name="infomelding" type="{http://nav.no/system/os/entiteter/infomelding}infomelding" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "simulerBeregningResponse", propOrder = {
    "simulering",
    "infomelding"
})
public class SimulerBeregningResponse {

    protected Beregning simulering;
    protected Infomelding infomelding;

    /**
     * Gets the value of the simulering property.
     * 
     * @return
     *     possible object is
     *     {@link Beregning }
     *     
     */
    public Beregning getSimulering() {
        return simulering;
    }

    /**
     * Sets the value of the simulering property.
     * 
     * @param value
     *     allowed object is
     *     {@link Beregning }
     *     
     */
    public void setSimulering(Beregning value) {
        this.simulering = value;
    }

    /**
     * Gets the value of the infomelding property.
     * 
     * @return
     *     possible object is
     *     {@link Infomelding }
     *     
     */
    public Infomelding getInfomelding() {
        return infomelding;
    }

    /**
     * Sets the value of the infomelding property.
     * 
     * @param value
     *     allowed object is
     *     {@link Infomelding }
     *     
     */
    public void setInfomelding(Infomelding value) {
        this.infomelding = value;
    }

}
