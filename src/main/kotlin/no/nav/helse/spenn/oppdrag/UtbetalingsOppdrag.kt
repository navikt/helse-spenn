package no.nav.helse.spenn.oppdrag

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsOppdrag(val id: String , val operasjon : AksjonsKode, val oppdragGjelder: String, val oppdragslinje : List<OppdragsLinje>)

data class OppdragsLinje(val id: String, val sats: BigDecimal, val satsTypeKode: SatsTypeKode, val datoFom : LocalDate,
                         val datoTom : LocalDate, val utbetalesTil: String)