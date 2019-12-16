package no.nav.helse.spenn.testsupport

import no.nav.helse.spenn.simulering.Simulering
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import java.math.BigDecimal
import java.time.LocalDate

val simuleringsresultat = SimuleringResult(
    status = SimuleringStatus.OK, simulering = Simulering(
        gjelderId = "12345678900",
        gjelderNavn = "Foo Bar", datoBeregnet = LocalDate.now(),
        totalBelop = BigDecimal.valueOf(1000), periodeList = emptyList()
    )
)
