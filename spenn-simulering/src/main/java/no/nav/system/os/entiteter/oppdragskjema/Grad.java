
package no.nav.system.os.entiteter.oppdragskjema;

import jakarta.xml.bind.annotation.*;
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
 *         &lt;element name="typeGrad">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="4"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="grad" type="{http://nav.no/system/os/entiteter/typer/simpleTypes}grad"/>
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
    "typeGrad",
    "grad"
})
@XmlRootElement(name = "grad")
public class Grad {

    @XmlElement(required = true)
    protected String typeGrad;
    @XmlElement(required = true)
    protected BigInteger grad;

    /**
     * Gets the value of the typeGrad property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeGrad() {
        return typeGrad;
    }

    /**
     * Sets the value of the typeGrad property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeGrad(String value) {
        this.typeGrad = value;
    }

    /**
     * Gets the value of the grad property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getGrad() {
        return grad;
    }

    /**
     * Sets the value of the grad property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setGrad(BigInteger value) {
        this.grad = value;
    }

}
