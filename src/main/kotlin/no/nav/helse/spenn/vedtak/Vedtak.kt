package no.nav.helse.spenn.vedtak

import java.time.LocalDate
import java.util.*

/**
 aggregert fra SPA sitt SykepengeVedtak-objekt
 */
data class Vedtak (
        val søknadId: UUID,
        val aktørId: String,
        val vedtaksperioder: List<Vedtaksperiode>,
        val maksDato : LocalDate
)

data class Vedtaksperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int = 100,
        val dagsats: Int,
        val fordeling: List<Fordeling>
)

data class Fordeling(
        val mottager: String,
        val andel: Int
)