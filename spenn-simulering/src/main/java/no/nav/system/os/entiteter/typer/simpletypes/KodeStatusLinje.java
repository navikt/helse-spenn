
package no.nav.system.os.entiteter.typer.simpletypes;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for kodeStatusLinje.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="kodeStatusLinje">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="OPPH"/>
 *     &lt;enumeration value="HVIL"/>
 *     &lt;enumeration value="SPER"/>
 *     &lt;enumeration value="REAK"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "kodeStatusLinje", namespace = "http://nav.no/system/os/entiteter/typer/simpleTypes")
@XmlEnum
public enum KodeStatusLinje {

    OPPH,
    HVIL,
    SPER,
    REAK;

    public String value() {
        return name();
    }

    public static KodeStatusLinje fromValue(String v) {
        return valueOf(v);
    }

}
