package no.nav.helse.spenn.simulering.api

import no.nav.helse.spenn.simulering.api.SimuleringRequest.Oppdrag
import no.nav.helse.spenn.simulering.api.SimuleringRequest.Oppdrag.Oppdragslinje.Klassekode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SimuleringRequestTest {

    @Test
    fun simuleringsperiode() {
        val request = SimuleringRequest(
            fødselsnummer = "",
            oppdrag = Oppdrag(
                fagområde = Oppdrag.Fagområde.ARBEIDSGIVERREFUSJON,
                fagsystemId = "",
                endringskode = Oppdrag.Endringskode.ENDRET,
                mottakerAvUtbetalingen = "",
                linjer = listOf(
                    Oppdrag.Oppdragslinje(
                        endringskode = Oppdrag.Endringskode.ENDRET,
                        fom = LocalDate.of(2018, 1, 10),
                        tom = LocalDate.of(2018, 1, 20),
                        satstype = Oppdrag.Oppdragslinje.Satstype.DAGLIG,
                        sats = 500,
                        grad = 100,
                        delytelseId = 1,
                        refDelytelseId = null,
                        refFagsystemId = null,
                        klassekodeFom = LocalDate.of(2018, 1, 10),
                        klassekode = Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                        opphørerFom = LocalDate.of(2018, 1, 10)
                    ),
                    Oppdrag.Oppdragslinje(
                        endringskode = Oppdrag.Endringskode.NY,
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 1, 20),
                        satstype = Oppdrag.Oppdragslinje.Satstype.DAGLIG,
                        sats = 500,
                        grad = 100,
                        delytelseId = 2,
                        refDelytelseId = null,
                        refFagsystemId = null,
                        klassekodeFom = LocalDate.of(2018, 1, 1),
                        klassekode = Klassekode.REFUSJON_IKKE_OPPLYSNINGSPLIKTIG,
                        opphørerFom = null
                    )
                )
            ),
            maksdato = null,
            saksbehandler = ""
        )

        assertEquals(LocalDate.of(2018, 1, 1), request.simuleringsperiodeFom)
        assertEquals(LocalDate.of(2018, 1, 20), request.simuleringsperiodeTom)
    }
}
