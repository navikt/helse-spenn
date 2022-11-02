
package no.nav.system.os.entiteter.typer.simpletypes;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for kodeArbeidsgiver.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="kodeArbeidsgiver">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="A"/>
 *     &lt;enumeration value="S"/>
 *     &lt;enumeration value="P"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "kodeArbeidsgiver", namespace = "http://nav.no/system/os/entiteter/typer/simpleTypes")
@XmlEnum
public enum KodeArbeidsgiver {

    A,
    S,
    P;

    public String value() {
        return name();
    }

    public static KodeArbeidsgiver fromValue(String v) {
        return valueOf(v);
    }

}
