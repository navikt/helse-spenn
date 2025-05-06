package no.nav.helse.spenn.simulering.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spenn.simulering.api.client.Attestant
import no.nav.helse.spenn.simulering.api.client.Enhet
import no.nav.helse.spenn.simulering.api.client.FradragTillegg
import no.nav.helse.spenn.simulering.api.client.Grad
import no.nav.helse.spenn.simulering.api.client.KodeStatusLinje
import no.nav.helse.spenn.simulering.api.client.Oppdrag
import no.nav.helse.spenn.simulering.api.client.Oppdragslinje
import no.nav.helse.spenn.simulering.api.client.RefusjonsInfo
import no.nav.helse.spenn.simulering.api.client.SimulerBeregningRequest
import no.nav.helse.spenn.simulering.api.client.Simulering
import no.nav.helse.spenn.simulering.api.client.SimuleringStatus
import no.nav.helse.spenn.simulering.api.client.SimuleringsPeriode
import no.nav.helse.spenn.simulering.api.client.SimuleringV2Service
import java.time.LocalDate

class Simuleringtjeneste(
    val simuleringV2Service: SimuleringV2Service
) {

    fun simulerOppdrag(simulering: SimuleringRequest): SimuleringResponse {
        val request = SimulerBeregningRequest(
            oppdrag = mapTilXMLRequest(simulering),
            simuleringsPeriode = SimuleringsPeriode(
                datoSimulerFom = simulering.simuleringsperiodeFom,
                datoSimulerTom = simulering.simuleringsperiodeTom
            ),
        )
        val response = simuleringV2Service.simulerOppdrag(request)
        return when (response.status) {
            SimuleringStatus.OK -> when (response.simulering) {
                null -> SimuleringResponse.OkMenTomt
                else -> SimuleringResponse.Ok(response.simulering)
            }
            SimuleringStatus.OPPDRAG_UR_ER_STENGT -> SimuleringResponse.OppdragsystemetErStengt
            SimuleringStatus.FUNKSJONELL_FEIL -> when (response.feilmelding) {
                null -> SimuleringResponse.TekniskFeil("Fikk en funksjonell feil uten feilmelding, det var uventet")
                else -> SimuleringResponse.FunksjonellFeil(response.feilmelding)
            }
            SimuleringStatus.TEKNISK_FEIL -> when (response.feilmelding) {
                null -> SimuleringResponse.TekniskFeil("Fikk en teknisk feil uten feilmelding, det var uventet")
                else -> SimuleringResponse.TekniskFeil(response.feilmelding)
            }
        }
    }

    private fun mapTilXMLRequest(simulering: SimuleringRequest): Oppdrag {
        val linjestrategi = when (simulering.oppdrag.fagområde) {
            SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON -> arbeidsgiverrefusjon(simulering.oppdrag.mottakerAvUtbetalingen.padStart(11, '0'), simulering.saksbehandler, simulering.maksdato)
            SimuleringRequest.Oppdrag.Fagområde.BRUKERUTBETALING -> brukerutbetaling(simulering.fødselsnummer, simulering.saksbehandler)
        }
        val linjer = simulering.oppdrag.linjer.map(linjestrategi)

        return Oppdrag(
            kodeFagomraade = when (simulering.oppdrag.fagområde) {
                SimuleringRequest.Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON -> "SPREF"
                SimuleringRequest.Oppdrag.Fagområde.BRUKERUTBETALING -> "SP"
            },
            kodeEndring = endringskode(simulering.oppdrag.endringskode),
            utbetFrekvens = "MND",
            fagsystemId = simulering.oppdrag.fagsystemId,
            oppdragGjelderId = simulering.fødselsnummer,
            saksbehId = simulering.saksbehandler,
            datoOppdragGjelderFom = LocalDate.EPOCH,
            enhet = listOf(Enhet(
                enhet = "8020",
                typeEnhet = "BOS",
                datoEnhetFom = LocalDate.EPOCH
            )),
            oppdragslinje = linjer.toMutableList()
        )
    }

    private fun arbeidsgiverrefusjon(organisasjonsnummer: String, saksbehandler: String, maksdato: LocalDate?) =
        { linje: SimuleringRequest.Oppdrag.Oppdragslinje ->
            felles(saksbehandler, linje).apply {
                refusjonsInfo = RefusjonsInfo(organisasjonsnummer, linje.fom, maksdato)
            }
        }
    private fun brukerutbetaling(fødselsnummer: String, saksbehandler: String) =
        { linje: SimuleringRequest.Oppdrag.Oppdragslinje ->
            felles(saksbehandler, linje).apply {
                utbetalesTilId = fødselsnummer
            }
        }

    private fun felles(saksbehandler: String, linje: SimuleringRequest.Oppdrag.Oppdragslinje): Oppdragslinje {
        return Oppdragslinje(
            delytelseId = linje.delytelseId.toString(),
            refDelytelseId = linje.refDelytelseId?.toString(),
            refFagsystemId = linje.refFagsystemId?.toString(),
            kodeEndringLinje = endringskode(linje.endringskode),
            kodeKlassifik = when (linje.klassekode) {
                SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG -> "SPREFAG-IOP"
                SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.REFUSJON_FERIEPENGER_IKKE_OPPLYSNINGSPLIKTIG -> "SPREFAGFER-IOP"
                SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SYKEPENGER_ARBEIDSTAKER_ORDINÆR -> "SPATORD"
                SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SYKEPENGER_ARBEIDSTAKER_FERIEPENGER -> "SPATFER"
                SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode.SELVSTENDIG_NÆRINGSDRIVENDE -> "SPSND-OP"
            },
            kodeStatusLinje = linje.opphørerFom?.let { KodeStatusLinje.OPPH },
            datoStatusFom = linje.opphørerFom,
            datoVedtakFom = linje.fom,
            datoVedtakTom = linje.tom,
            sats = linje.sats,
            fradragTillegg = FradragTillegg.T,
            typeSats = when (linje.satstype) {
                SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.DAGLIG -> "DAG"
                SimuleringRequest.Oppdrag.Oppdragslinje.Satstype.ENGANGS -> "ENG"
            },
            saksbehId = saksbehandler,
            brukKjoreplan = "N",
            grad = linje.grad?.let { listOf(Grad("UFOR", linje.grad)) } ?: emptyList(),
            attestant = listOf(Attestant(saksbehandler))
        )
    }

    private fun endringskode(endringskode: SimuleringRequest.Oppdrag.Endringskode) =
        when (endringskode) {
            SimuleringRequest.Oppdrag.Endringskode.NY -> "NY"
            SimuleringRequest.Oppdrag.Endringskode.ENDRET -> "ENDR"
            SimuleringRequest.Oppdrag.Endringskode.IKKE_ENDRET -> "UEND"
        }
}

sealed interface SimuleringResponse {
    data class Ok(val simulering: Simulering) : SimuleringResponse
    data object OkMenTomt : SimuleringResponse
    data object OppdragsystemetErStengt : SimuleringResponse
    data class FunksjonellFeil(val feilmelding: String) : SimuleringResponse
    data class TekniskFeil(val feilmelding: String) : SimuleringResponse
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimuleringRequest(
    val fødselsnummer: String,
    val oppdrag: Oppdrag,
    val maksdato: LocalDate?,
    val saksbehandler: String
) {
    val simuleringsperiodeFom get() = oppdrag.linjer.minOf { it.fom }
    val simuleringsperiodeTom get() = oppdrag.linjer.last().tom
    data class Oppdrag(
        val fagområde: Fagområde,
        val fagsystemId: String,
        val endringskode: Endringskode,
        val mottakerAvUtbetalingen: String,
        val linjer: List<Oppdragslinje>
    ) {
        enum class Fagområde {
            ARBEIDSGIVERREFUSJON,
            BRUKERUTBETALING
        }
        enum class Endringskode {
            NY, ENDRET, IKKE_ENDRET
        }

        data class Oppdragslinje(
            val endringskode: Endringskode,
            val fom: LocalDate,
            val tom: LocalDate,
            val satstype: Satstype,
            val sats: Int,
            val grad: Int?,

            val delytelseId: Int,
            val refDelytelseId: Int?,
            val refFagsystemId: String?,

            val klassekode: Klassekode,
            val opphørerFom: LocalDate?
        ) {
            enum class Satstype {
                DAGLIG, ENGANGS
            }
            enum class Klassekode {
                REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                REFUSJON_FERIEPENGER_IKKE_OPPLYSNINGSPLIKTIG,
                SYKEPENGER_ARBEIDSTAKER_ORDINÆR,
                SYKEPENGER_ARBEIDSTAKER_FERIEPENGER,
                SELVSTENDIG_NÆRINGSDRIVENDE
            }
        }
    }
}
