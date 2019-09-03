package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Vedtak (
        val søknadId: UUID,
        val aktørId: String,
        val vedtaksperioder: List<Vedtaksperiode>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Vedtaksperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int = 100,
        val dagsats: Int,
        val fordeling: List<Fordeling>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fordeling(
        val mottager: String,
        val andel: Int
)