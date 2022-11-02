
package no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for simulerBeregningRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="simulerBeregningRequest">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="oppdrag" type="{http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceServiceTypes}oppdrag"/>
 *         &lt;element name="simuleringsPeriode">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="datoSimulerFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *                   &lt;element name="datoSimulerTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "simulerBeregningRequest", propOrder = {
    "oppdrag",
    "simuleringsPeriode"
})
public class SimulerBeregningRequest {

    @XmlElement(required = true)
    protected Oppdrag oppdrag;
    @XmlElement(required = true)
    protected SimuleringsPeriode simuleringsPeriode;

    /**
     * Gets the value of the oppdrag property.
     * 
     * @return
     *     possible object is
     *     {@link Oppdrag }
     *     
     */
    public Oppdrag getOppdrag() {
        return oppdrag;
    }

    /**
     * Sets the value of the oppdrag property.
     * 
     * @param value
     *     allowed object is
     *     {@link Oppdrag }
     *     
     */
    public void setOppdrag(Oppdrag value) {
        this.oppdrag = value;
    }

    /**
     * Gets the value of the simuleringsPeriode property.
     * 
     * @return
     *     possible object is
     *     {@link SimuleringsPeriode }
     *     
     */
    public SimuleringsPeriode getSimuleringsPeriode() {
        return simuleringsPeriode;
    }

    /**
     * Sets the value of the simuleringsPeriode property.
     * 
     * @param value
     *     allowed object is
     *     {@link SimuleringsPeriode }
     *     
     */
    public void setSimuleringsPeriode(SimuleringsPeriode value) {
        this.simuleringsPeriode = value;
    }


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
     *         &lt;element name="datoSimulerFom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
     *         &lt;element name="datoSimulerTom" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}dato" minOccurs="0"/>
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
        "datoSimulerFom",
        "datoSimulerTom"
    })
    public static class SimuleringsPeriode {

        protected String datoSimulerFom;
        protected String datoSimulerTom;

        /**
         * Gets the value of the datoSimulerFom property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDatoSimulerFom() {
            return datoSimulerFom;
        }

        /**
         * Sets the value of the datoSimulerFom property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDatoSimulerFom(String value) {
            this.datoSimulerFom = value;
        }

        /**
         * Gets the value of the datoSimulerTom property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDatoSimulerTom() {
            return datoSimulerTom;
        }

        /**
         * Sets the value of the datoSimulerTom property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDatoSimulerTom(String value) {
            this.datoSimulerTom = value;
        }

    }

}
