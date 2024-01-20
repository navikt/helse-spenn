package no.nav.helse.spenn.oppdrag

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.absoluteValue

internal class OppdragBuilder(
    private val utbetalingId: UUID,
    private val utbetalingslinjer: Utbetalingslinjer,
    private val avstemmingsnøkkel: Long,
    tidspunkt: LocalDateTime = LocalDateTime.now()
) {
    private val linjeStrategy: (Utbetalingslinjer.Utbetalingslinje) -> OppdragsLinje150Dto = when (utbetalingslinjer) {
        is Utbetalingslinjer.RefusjonTilArbeidsgiver -> ::refusjonTilArbeidsgiver
        is Utbetalingslinjer.UtbetalingTilBruker -> ::utbetalingTilBruker
    }

    private val oppdragslinjer = mutableListOf<OppdragsLinje150Dto>()
    private val oppdrag110 = Oppdrag110Dto(
        kodeFagomraade = utbetalingslinjer.fagområde,
        kodeEndring = EndringskodeDto.valueOf(utbetalingslinjer.endringskode),
        fagsystemId = utbetalingslinjer.fagsystemId.trim(),
        oppdragGjelderId = utbetalingslinjer.fødselsnummer,
        saksbehId = utbetalingslinjer.saksbehandler,
        kodeAksjon = "1",
        utbetFrekvens = "MND",
        datoOppdragGjelderFom = LocalDate.EPOCH,
        avstemming115 = Avstemming115Dto(
            nokkelAvstemming = avstemmingsnøkkel,
            tidspktMelding = tidspunkt,
            kodeKomponent = "SP",
        ),
        oppdragsEnhet120 = listOf(
            OppdragsEnhet120Dto(
                enhet = "8020",
                typeEnhet = "BOS",
                datoEnhetFom = LocalDate.EPOCH
            )
        ),
        oppdragsLinje150 = oppdragslinjer
    )

    fun build(): OppdragDto {
        utbetalingslinjer.forEach { oppdragslinjer.add(linjeStrategy(it)) }
        return OppdragDto(
            oppdrag110 = this@OppdragBuilder.oppdrag110
        )
    }

    private fun refusjonTilArbeidsgiver(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) =
        nyLinje(utbetalingslinje).apply {
            refusjonsinfo156 = Refusjonsinfo156Dto(
                refunderesId = utbetalingslinjer.mottaker.padStart(11, '0'),
                datoFom = datoVedtakFom,
                maksDato = utbetalingslinjer.maksdato
            )
        }

    private fun utbetalingTilBruker(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) =
        nyLinje(utbetalingslinje).apply {
            utbetalesTilId = utbetalingslinjer.mottaker
        }

    private fun nyLinje(utbetalingslinje: Utbetalingslinjer.Utbetalingslinje) = OppdragsLinje150Dto(
        delytelseId = utbetalingslinje.delytelseId,
        refDelytelseId = utbetalingslinje.refDelytelseId,
        refFagsystemId = utbetalingslinje.refFagsystemId?.trim(),
        kodeEndringLinje = EndringskodeDto.valueOf(utbetalingslinje.endringskode),
        kodeKlassifik = utbetalingslinje.klassekode,
        datoVedtakFom = utbetalingslinje.fom,
        datoVedtakTom = utbetalingslinje.tom,
        kodeStatusLinje = utbetalingslinje.statuskode?.let { StatuskodeLinjeDto.valueOf(it) },
        datoStatusFom = utbetalingslinje.datoStatusFom,
        sats = utbetalingslinje.sats.absoluteValue,
        henvisning = "$utbetalingId",
        fradragTillegg = if (utbetalingslinje.sats >= 0) FradragTilleggDto.T else FradragTilleggDto.F,
        typeSats = SatstypeDto.valueOf(utbetalingslinje.satstype),
        saksbehId = utbetalingslinjer.saksbehandler,
        brukKjoreplan = "N",
        grad170 = listOfNotNull(
            if (utbetalingslinje.grad != null) {
                Grad170Dto(
                    typeGrad = "UFOR",
                    grad = utbetalingslinje.grad
                )
            } else null
        ),
        attestant180 = listOf(
            Attestant180Dto(
                attestantId = utbetalingslinjer.saksbehandler
            )
        )
    )
}

enum class EndringskodeDto {
    NY, UEND, ENDR
}
enum class FradragTilleggDto {
    F, T
}

enum class StatuskodeLinjeDto {
    OPPH,
    HVIL,
    SPER,
    REAK;
}

@JsonRootName(value = "Oppdrag", namespace = "http://www.trygdeetaten.no/skjema/oppdrag")
data class OppdragDto(
    @JsonProperty(value = "oppdrag-110")
    val oppdrag110: Oppdrag110Dto,

    @JsonProperty(value = "mmel")
    val mmel: MmelDto? = null
)

data class MmelDto(
    @JsonProperty(value = "alvorlighetsgrad")
    val alvorlighetsgrad: AlvorlighetsgradDto,
    @JsonProperty(value = "beskrMelding")
    val beskrMelding: String?,
    @JsonProperty(value = "kodeMelding")
    val kodeMelding: String?
)

enum class AlvorlighetsgradDto(
    @JsonValue val verdi: String
) {
    AKSEPTERT("00"),
    AKSEPTERT_MED_FEIL("04"),
    AVVIST("08"),
    FEIL("12"),
    @JsonEnumDefaultValue // forutsetter mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    UKJENT("??")
}

data class Oppdrag110Dto(
    @JsonProperty(value = "kodeFagomraade")
    val kodeFagomraade: String,
    @JsonProperty(value = "kodeEndring")
    val kodeEndring: EndringskodeDto,
    @JsonProperty(value = "fagsystemId")
    val fagsystemId: String,
    @JsonProperty(value = "oppdragGjelderId")
    val oppdragGjelderId: String,
    @JsonProperty(value = "saksbehId")
    val saksbehId: String,
    @JsonProperty(value = "kodeAksjon")
    val kodeAksjon: String,
    @JsonProperty(value = "utbetFrekvens")
    val utbetFrekvens: String,
    @JsonProperty(value = "datoOppdragGjelderFom")
    val datoOppdragGjelderFom: LocalDate?,
    @JsonProperty(value = "avstemming-115")
    val avstemming115: Avstemming115Dto,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty(value = "oppdrags-enhet-120")
    val oppdragsEnhet120: List<OppdragsEnhet120Dto>,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty(value = "oppdrags-linje-150")
    val oppdragsLinje150: List<OppdragsLinje150Dto>
)

data class Avstemming115Dto(
    @JsonProperty(value = "nokkelAvstemming")
    val nokkelAvstemming: Long,
    @JsonProperty(value = "tidspktMelding")
    val tidspktMelding: LocalDateTime,
    @JsonProperty(value = "kodeKomponent")
    val kodeKomponent: String
)

data class OppdragsEnhet120Dto(
    @JsonProperty(value = "enhet")
    val enhet: String,
    @JsonProperty(value = "typeEnhet")
    val typeEnhet: String,
    @JsonProperty(value = "datoEnhetFom")
    val datoEnhetFom: LocalDate?
)

data class Refusjonsinfo156Dto(
    @JsonProperty(value = "refunderesId")
    val refunderesId: String,
    @JsonProperty(value = "datoFom")
    val datoFom: LocalDate,
    @JsonProperty(value = "maksDato")
    val maksDato: LocalDate?
)

data class OppdragsLinje150Dto(
    @JsonProperty(value = "delytelseId")
    val delytelseId: Int,
    @JsonProperty(value = "refDelytelseId")
    val refDelytelseId: Int?,
    @JsonProperty(value = "refFagsystemId")
    val refFagsystemId: String?,
    @JsonProperty(value = "kodeEndringLinje")
    val kodeEndringLinje: EndringskodeDto,
    @JsonProperty(value = "kodeKlassifik")
    val kodeKlassifik: String,
    @JsonProperty(value = "datoVedtakFom")
    val datoVedtakFom: LocalDate,
    @JsonProperty(value = "datoVedtakTom")
    val datoVedtakTom: LocalDate,
    @JsonProperty(value = "kodeStatusLinje")
    val kodeStatusLinje: StatuskodeLinjeDto?,
    @JsonProperty(value = "datoStatusFom")
    val datoStatusFom: LocalDate?,
    @JsonProperty(value = "sats")
    val sats: Int,
    @JsonProperty(value = "henvisning")
    val henvisning: String,
    @JsonProperty(value = "fradragTillegg")
    val fradragTillegg: FradragTilleggDto,
    @JsonProperty(value = "typeSats")
    val typeSats: SatstypeDto,
    @JsonProperty(value = "saksbehId")
    val saksbehId: String,
    @JsonProperty(value = "brukKjoreplan")
    val brukKjoreplan: String,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty(value = "grad-170")
    val grad170: List<Grad170Dto> = emptyList(),
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty(value = "attestant-180")
    val attestant180: List<Attestant180Dto>
) {
    @JsonProperty(value = "refusjonsinfo-156")
    var refusjonsinfo156: Refusjonsinfo156Dto? = null
    @JsonProperty(value = "utbetalesTilId")
    var utbetalesTilId: String? = null
}

enum class SatstypeDto {
    ENG, DAG
}

data class Grad170Dto(
    @JsonProperty(value = "typeGrad")
    val typeGrad: String,
    @JsonProperty(value = "grad")
    val grad: Int
)

data class Attestant180Dto(
    @JsonProperty(value = "attestantId")
    val attestantId: String
)
