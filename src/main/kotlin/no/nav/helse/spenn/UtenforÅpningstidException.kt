package no.nav.helse.spenn

import javax.xml.ws.soap.SOAPFaultException

class UtenforÅpningstidException(message: String, cause: SOAPFaultException) : RuntimeException(message, cause) {

}
