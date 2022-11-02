
package no.nav.virksomhet.tjenester.avstemming.meldinger.v1;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Fortegn.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Fortegn">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="T"/>
 *     &lt;enumeration value="F"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Fortegn")
@XmlEnum
public enum Fortegn {


    /**
     * Tillegg
     * 
     */
    T,

    /**
     * Fradrag
     * 
     */
    F;

    public String value() {
        return name();
    }

    public static Fortegn fromValue(String v) {
        return valueOf(v);
    }

}
