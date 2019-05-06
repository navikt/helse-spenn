package no.nav.helse.spenn.rest

import no.nav.helse.spenn.dao.OppdragStateService
import no.nav.helse.spenn.vedtak.OppdragStateDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/v1/oppdragstate")
class OppdragStateController(val oppdragStateService: OppdragStateService) {

    @GetMapping("/{soknadId}", produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE) )
    fun getBySoknadId(@PathVariable soknadId: UUID): ResponseEntity<OppdragStateDTO> {
        try {
            return ResponseEntity.ok(oppdragStateService.fetchOppdragState(soknadId))
        }
        catch (e: Exception) {
            throw ResponseStatusException(NOT_FOUND)
        }
    }

}