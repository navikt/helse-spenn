//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.02.26 at 08:29:59 AM UTC 
//


package no.nav.trygdeetaten.skjema.oppdrag;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for TfradragTillegg.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="TfradragTillegg"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="F"/&gt;
 *     &lt;enumeration value="T"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "TfradragTillegg")
@XmlEnum
public enum TfradragTillegg {

    F,
    T;

    public String value() {
        return name();
    }

    public static TfradragTillegg fromValue(String v) {
        return valueOf(v);
    }

}