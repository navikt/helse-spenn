
package no.nav.virksomhet.tjenester.avstemming.meldinger.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for DetaljType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DetaljType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="VARS"/>
 *     &lt;enumeration value="AVVI"/>
 *     &lt;enumeration value="MANG"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "DetaljType")
@XmlEnum
public enum DetaljType {


    /**
     * Godkjent med varsel
     * 
     */
    VARS,

    /**
     * Avvist
     * 
     */
    AVVI,

    /**
     * Manglende kvittering
     * 
     */
    MANG;

    public String value() {
        return name();
    }

    public static DetaljType fromValue(String v) {
        return valueOf(v);
    }

}
