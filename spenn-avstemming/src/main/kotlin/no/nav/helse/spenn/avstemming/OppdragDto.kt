package no.nav.helse.spenn.avstemming

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

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

        fun detaljer(liste: List<OppdragDto>) = liste.mapNotNull { it.somDetalj() }

        fun totaldata(liste: List<OppdragDto>) = Totaldata().apply {
            liste.summer { antall, beløp, fortegn ->
                totalAntall = antall
                totalBelop = beløp
                this.fortegn = fortegn
            }
        }

        fun grunnlagsdata(liste: List<OppdragDto>) = Grunnlagsdata().apply {
            val beløpEtterStatus = liste.groupBy { it.status }
            beløpEtterStatus.summer(Oppdragstatus.AKSEPTERT) { antall, beløp, fortegn ->
                godkjentAntall = antall
                godkjentBelop = beløp
                godkjentFortegn = fortegn
            }
            beløpEtterStatus.summer(Oppdragstatus.AKSEPTERT_MED_VARSEL) { antall, beløp, fortegn ->
                varselAntall = antall
                varselBelop = beløp
                varselFortegn = fortegn
            }
            beløpEtterStatus.summer(Oppdragstatus.AVVIST) { antall, beløp, fortegn ->
                avvistAntall = antall
                avvistBelop = beløp
                avvistFortegn = fortegn
            }
            beløpEtterStatus.summer(Oppdragstatus.MANGELFULL) { antall, beløp, fortegn ->
                manglerAntall = antall
                manglerBelop = beløp
                manglerFortegn = fortegn
            }
        }

        private fun List<OppdragDto>.summer(block: (Int, BigDecimal, Fortegn) -> Unit) {
            totalbeløp(this)
                .also { block(size, it.absoluteValue.toBigDecimal(), if (it >= 0) Fortegn.T else Fortegn.F) }
        }

        private fun Map<Oppdragstatus, List<OppdragDto>>.summer(
            status: Oppdragstatus,
            block: (Int, BigDecimal, Fortegn) -> Unit
        ) {
            this[status]?.summer(block)
        }

        private fun totalbeløp(liste: List<OppdragDto>) = liste.sumOf { it.totalbeløp }
    }

    private fun somDetalj(): Detaljdata? {
        return Detaljdata().apply {
            detaljType = detaljType() ?: return null
            offnr = fødselsnummer
            avleverendeTransaksjonNokkel = fagsystemId.trim()
            tidspunkt = tidsstempel.format(opprettet)
            if (status in setOf(Oppdragstatus.AVVIST, Oppdragstatus.AKSEPTERT_MED_VARSEL)) {
                meldingKode = kodemelding
                alvorlighetsgrad = this@OppdragDto.alvorlighetsgrad
                tekstMelding = beskrivendemelding
            }
        }
    }

    private fun detaljType() = when (status) {
        Oppdragstatus.MANGELFULL -> DetaljType.MANG
        Oppdragstatus.AVVIST -> DetaljType.AVVI
        Oppdragstatus.AKSEPTERT_MED_VARSEL -> DetaljType.VARS
        else -> null
    }
}
