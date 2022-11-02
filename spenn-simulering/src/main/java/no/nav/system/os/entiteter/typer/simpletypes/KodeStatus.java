
package no.nav.system.os.entiteter.typer.simpletypes;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for kodeStatus.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="kodeStatus">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="NY"/>
 *     &lt;enumeration value="LOPE"/>
 *     &lt;enumeration value="HVIL"/>
 *     &lt;enumeration value="SPER"/>
 *     &lt;enumeration value="IKAT"/>
 *     &lt;enumeration value="ATTE"/>
 *     &lt;enumeration value="ANNU"/>
 *     &lt;enumeration value="OPPH"/>
 *     &lt;enumeration value="FBER"/>
 *     &lt;enumeration value="REAK"/>
 *     &lt;enumeration value="KORR"/>
 *     &lt;enumeration value="FEIL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "kodeStatus", namespace = "http://nav.no/system/os/entiteter/typer/simpleTypes")
@XmlEnum
public enum KodeStatus {

    NY,
    LOPE,
    HVIL,
    SPER,
    IKAT,
    ATTE,
    ANNU,
    OPPH,
    FBER,
    REAK,
    KORR,
    FEIL;

    public String value() {
        return name();
    }

    public static KodeStatus fromValue(String v) {
        return valueOf(v);
    }

}
