package no.nav.helse.spenn.avstemming

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OppdragDto(
    private val avstemmingsnøkkel: Long,
    private val fødselsnummer: String,
    private val fagsystemId: String,
    private val opprettet: LocalDateTime,
    private val status: Oppdragstatus,
    private val totalbeløp: Int,
    private val alvorlighetsgrad: String?,
    private val kodemelding: String?,
    private val beskrivendemelding: String?
) {
    internal companion object {
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

        fun periode(liste: List<OppdragDto>): ClosedRange<LocalDateTime> {
            check(liste.isNotEmpty())
            return object : ClosedRange<LocalDateTime> {
                override val start = liste.minOf { it.opprettet }
                override val endInclusive = liste.maxOf { it.opprettet }
            }
        }

        fun avstemmingsperiode(liste: List<OppdragDto>): ClosedRange<Long> {
            check(liste.isNotEmpty())
            return LongRange(
                liste.minOf { it.avstemmingsnøkkel },
                liste.maxOf { it.avstemmingsnøkkel }
            )
        }
    }
}
