package no.nav.helse.spenn

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.pow

internal object Avstemmingsnøkkel {

    fun periode(dag: LocalDate): ClosedRange<Long> = with(ZoneId.systemDefault()) {
        opprett(dag.atStartOfDay(this).toInstant())..opprett(dag.atEndOfDay(this).toInstant())
    }

    fun opprett(tidspunkt: Instant = Instant.now()) = tidspunkt.epochSecond * 10.0.pow(9).toLong() + tidspunkt.nano

    private fun LocalDate.atEndOfDay(zoneId: ZoneId) = atTime(23, 59, 59, 999999999).atZone(zoneId)
}
