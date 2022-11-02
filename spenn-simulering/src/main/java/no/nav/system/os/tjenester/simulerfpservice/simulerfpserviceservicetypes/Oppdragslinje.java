
package no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes;

import no.nav.system.os.entiteter.oppdragskjema.*;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for oppdragslinje complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="oppdragslinje">
 *   &lt;complexContent>
 *     &lt;extension base="{http://nav.no/system/os/entiteter/oppdragSkjema}oppdragslinje">
 *       &lt;sequence>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}refusjonsInfo" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}tekst" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}enhet" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}grad" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}attestant" maxOccurs="50" minOccurs="0"/>
 *         &lt;element ref="{http://nav.no/system/os/entiteter/oppdragSkjema}valuta" maxOccurs="50" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "oppdragslinje", propOrder = {
    "refusjonsInfo",
    "tekst",
    "enhet",
    "grad",
    "attestant",
    "valuta"
})
public class Oppdragslinje
    extends no.nav.system.os.entiteter.oppdragskjema.Oppdragslinje
{

    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected RefusjonsInfo refusjonsInfo;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Tekst> tekst;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Enhet> enhet;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Grad> grad;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Attestant> attestant;
    @XmlElement(namespace = "http://nav.no/system/os/entiteter/oppdragSkjema")
    protected List<Valuta> valuta;

    /**
     * Gets the value of the refusjonsInfo property.
     * 
     * @return
     *     possible object is
     *     {@link RefusjonsInfo }
     *     
     */
    public RefusjonsInfo getRefusjonsInfo() {
        return refusjonsInfo;
    }

    /**
     * Sets the value of the refusjonsInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link RefusjonsInfo }
     *     
     */
    public void setRefusjonsInfo(RefusjonsInfo value) {
        this.refusjonsInfo = value;
    }

    /**
     * Gets the value of the tekst property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tekst property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTekst().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tekst }
     * 
     * 
     */
    public List<Tekst> getTekst() {
        if (tekst == null) {
            tekst = new ArrayList<Tekst>();
        }
        return this.tekst;
    }

    /**
     * Gets the value of the enhet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the enhet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEnhet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Enhet }
     * 
     * 
     */
    public List<Enhet> getEnhet() {
        if (enhet == null) {
            enhet = new ArrayList<Enhet>();
        }
        return this.enhet;
    }

    /**
     * Gets the value of the grad property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the grad property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGrad().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Grad }
     * 
     * 
     */
    public List<Grad> getGrad() {
        if (grad == null) {
            grad = new ArrayList<Grad>();
        }
        return this.grad;
    }

    /**
     * Gets the value of the attestant property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attestant property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttestant().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Attestant }
     * 
     * 
     */
    public List<Attestant> getAttestant() {
        if (attestant == null) {
            attestant = new ArrayList<Attestant>();
        }
        return this.attestant;
    }

    /**
     * Gets the value of the valuta property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the valuta property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValuta().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Valuta }
     * 
     * 
     */
    public List<Valuta> getValuta() {
        if (valuta == null) {
            valuta = new ArrayList<Valuta>();
        }
        return this.valuta;
    }

}
