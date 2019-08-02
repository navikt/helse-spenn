package no.nav.helse.spenn.oppdrag

import no.nav.helse.spenn.dao.OppdragStateStatus
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OppdragStatusMappingTest {

    companion object {
        private fun oppdragMedAlvorlighetsgrad(wantedAlvorlighetsgrad: String) : Oppdrag =
                Oppdrag().apply {
                    mmel = Mmel().apply { alvorlighetsgrad = wantedAlvorlighetsgrad }
                    oppdrag110 = Oppdrag110()
                }
    }

    @Test
    fun testStatusMapper() {
        val feilMelding = "OH-oh! Mapping fra alvorlighetsgrad til status er endret. Sjekk om det er andre (mockede) tester som nå også må oppdateres!"
        assertEquals(OppdragStateStatus.FERDIG, OppdragMQReceiver.mapStatus(oppdragMedAlvorlighetsgrad("00")), feilMelding)
        assertEquals(OppdragStateStatus.FERDIG, OppdragMQReceiver.mapStatus(oppdragMedAlvorlighetsgrad("04")), feilMelding)
        assertEquals(OppdragStateStatus.FEIL, OppdragMQReceiver.mapStatus(oppdragMedAlvorlighetsgrad("08")), feilMelding)
        assertEquals(OppdragStateStatus.FEIL, OppdragMQReceiver.mapStatus(oppdragMedAlvorlighetsgrad("12")), feilMelding)
    }
}