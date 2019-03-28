package no.nav.helse.spenn.oppdrag

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppdrag")
class OppdragRest(val mqSender: OppdragMQSender, val jaxb : JAXBOppdrag) {

    @PostMapping
    fun sendOppdrag(@RequestBody oppdragXml: String) {
        mqSender.sendOppdrag(jaxb.toOppdrag(oppdragXml))
    }

}