package no.nav.helse.spenn.vedtak

import java.math.BigDecimal
import java.time.LocalDate

/**
 aggregert fra SPA sitt SykepengeVedtak-objekt
 */
data class Vedtak (
    val søknadId: String,
    val fnr: String,
    val vedtaksperioder: List<Vedtaksperiode>
)

data class Vedtaksperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: BigDecimal,
        val fordeling: List<Fordeling>
)

data class Fordeling(
        val mottager: String,
        val andel: Int
)