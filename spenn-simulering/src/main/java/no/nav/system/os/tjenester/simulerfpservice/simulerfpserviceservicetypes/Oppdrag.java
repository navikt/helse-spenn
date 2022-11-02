
package no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes;

import no.nav.system.os.entiteter.oppdragskjema.Avstemmingsnokkel;
import no.nav.system.os.entiteter.oppdragskjema.Bilagstype;
import no.nav.system.os.entiteter.oppdragskjema.Ompostering;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for oppdrag complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="oppdrag">
 *   &lt;complexContent>
 *     &lt;extension base="{http://nav.no/system/os/entiteter/oppdragSkjema}oppdrag">
 *       &lt;sequence>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}bilagstype" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}avstemmingsnokkel" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}ompostering" minOccurs="0"/>
 *         &lt;element name="oppdragslinje" type="{http://nav.no/system/os/tjenester/simulerFpService/simulerFpServiceServiceTypes}oppdragslinje" maxOccurs="1400" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "oppdrag", propOrder = {
    "bilagstype",
    "avstemmingsnokkel",
    "ompostering",
    "oppdragslinje"
})
public class Oppdrag
    extends no.nav.system.os.entiteter.oppdragskjema.Oppdrag
{

    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Bilagstype> bilagstype;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Avstemmingsnokkel> avstemmingsnokkel;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected Ompostering ompostering;
    protected List<Oppdragslinje> oppdragslinje;

    /**
     * Gets the value of the bilagstype property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bilagstype property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBilagstype().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Bilagstype }
     * 
     * 
     */
    public List<Bilagstype> getBilagstype() {
        if (bilagstype == null) {
            bilagstype = new ArrayList<Bilagstype>();
        }
        return this.bilagstype;
    }

    /**
     * Gets the value of the avstemmingsnokkel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the avstemmingsnokkel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAvstemmingsnokkel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Avstemmingsnokkel }
     * 
     * 
     */
    public List<Avstemmingsnokkel> getAvstemmingsnokkel() {
        if (avstemmingsnokkel == null) {
            avstemmingsnokkel = new ArrayList<Avstemmingsnokkel>();
        }
        return this.avstemmingsnokkel;
    }

    /**
     * Gets the value of the ompostering property.
     * 
     * @return
     *     possible object is
     *     {@link Ompostering }
     *     
     */
    public Ompostering getOmpostering() {
        return ompostering;
    }

    /**
     * Sets the value of the ompostering property.
     * 
     * @param value
     *     allowed object is
     *     {@link Ompostering }
     *     
     */
    public void setOmpostering(Ompostering value) {
        this.ompostering = value;
    }

    /**
     * Gets the value of the oppdragslinje property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the oppdragslinje property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOppdragslinje().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Oppdragslinje }
     * 
     * 
     */
    public List<Oppdragslinje> getOppdragslinje() {
        if (oppdragslinje == null) {
            oppdragslinje = new ArrayList<Oppdragslinje>();
        }
        return this.oppdragslinje;
    }

}
