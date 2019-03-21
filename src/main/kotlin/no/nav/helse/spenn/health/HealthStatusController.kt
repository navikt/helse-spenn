package no.nav.helse.spenn.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthStatusController {

    @GetMapping("/internal/isalive")
    fun isAlive(): String {
        return "ALIVE"
    }

    @GetMapping("/internal/isready")
    fun isReady(): String {
        return "READY"
    }

}