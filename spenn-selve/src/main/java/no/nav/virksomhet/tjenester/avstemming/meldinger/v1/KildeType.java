
package no.nav.virksomhet.tjenester.avstemming.meldinger.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for KildeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="KildeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="AVLEV"/>
 *     &lt;enumeration value="MOTT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "KildeType")
@XmlEnum
public enum KildeType {


    /**
     * Avleverende komponent
     * 
     */
    AVLEV,

    /**
     * Mottakende komponent
     * 
     */
    MOTT;

    public String value() {
        return name();
    }

    public static KildeType fromValue(String v) {
        return valueOf(v);
    }

}
