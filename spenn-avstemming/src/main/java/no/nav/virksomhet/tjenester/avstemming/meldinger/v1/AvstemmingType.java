
package no.nav.virksomhet.tjenester.avstemming.meldinger.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for AvstemmingType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AvstemmingType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="GRSN"/>
 *     &lt;enumeration value="KONS"/>
 *     &lt;enumeration value="PERI"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "AvstemmingType")
@XmlEnum
public enum AvstemmingType {


    /**
     * Grensesnittavstemming
     * 
     */
    GRSN,

    /**
     * Konsistensavstemming
     * 
     */
    KONS,

    /**
     * Periodeavstemming
     * 
     */
    PERI;

    public String value() {
        return name();
    }

    public static AvstemmingType fromValue(String v) {
        return valueOf(v);
    }

}
