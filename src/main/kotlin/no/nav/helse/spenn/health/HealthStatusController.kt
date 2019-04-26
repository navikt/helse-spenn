package no.nav.helse.spenn.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthStatusController {

    @GetMapping("/internal/isAlive")
    fun isAlive(): String {
        return "ALIVE"
    }

    @GetMapping("/internal/isReady")
    fun isReady(): String {
        return "READY"
    }

}