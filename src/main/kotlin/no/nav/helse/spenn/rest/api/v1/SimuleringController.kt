package no.nav.helse.spenn.rest.api.v1

import no.nav.helse.spenn.oppdrag.OppdragStateDTO
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.vedtak.Vedtak
import no.nav.helse.spenn.vedtak.fnr.AktørTilFnrMapper
import no.nav.helse.spenn.vedtak.tilUtbetaling
import no.nav.security.oidc.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@RequestMapping("/api/v1/simulering")
class SimuleringController(val simuleringService: SimuleringService,
                           val aktørTilFnrMapper: AktørTilFnrMapper) {

    @PostMapping()
    fun runSimulering(@RequestBody vedtak: Vedtak): SimuleringResult? {
        val oppdrag = OppdragStateDTO(soknadId = vedtak.soknadId,
                utbetalingsOppdrag = vedtak.tilUtbetaling(aktørTilFnrMapper.tilFnr(vedtak.aktorId)))
        return simuleringService.runSimulering(oppdrag).simuleringResult
    }

}