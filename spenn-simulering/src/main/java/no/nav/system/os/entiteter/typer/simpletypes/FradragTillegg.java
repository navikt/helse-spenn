
package no.nav.system.os.entiteter.typer.simpletypes;


import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>Java class for fradragTillegg.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="fradragTillegg">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="F"/>
 *     &lt;enumeration value="T"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "fradragTillegg", namespace = "http://nav.no/system/os/entiteter/typer/simpleTypes")
@XmlEnum
public enum FradragTillegg {

    F,
    T;

    public String value() {
        return name();
    }

    public static FradragTillegg fromValue(String v) {
        return valueOf(v);
    }

}
