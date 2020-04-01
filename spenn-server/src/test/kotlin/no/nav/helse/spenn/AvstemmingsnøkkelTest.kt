package no.nav.helse.spenn

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal class AvstemmingsnøkkelTest {

    @Test
    fun `avstemmingsnøkkel er et tall`() {
        val idag = LocalDate.now()
        val nå = idag
            .atTime(LocalTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val igår = idag
            .minusDays(1)
            .atTime(LocalTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val imorgen = idag
            .plusDays(1)
            .atTime(LocalTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val periode = Avstemmingsnøkkel.periode(idag)
        val avstemmingsnøkkel = Avstemmingsnøkkel.opprett(nå)
        val avstemmingsnøkkelIgår = Avstemmingsnøkkel.opprett(igår)
        val avstemmingsnøkkelImorgen = Avstemmingsnøkkel.opprett(imorgen)
        assertTrue(avstemmingsnøkkel in periode)
        assertFalse(avstemmingsnøkkelIgår in periode)
        assertTrue(avstemmingsnøkkelIgår < periode.start)
        assertFalse(avstemmingsnøkkelImorgen in periode)
        assertTrue(avstemmingsnøkkelImorgen > periode.endInclusive)
    }

    @Test
    fun `ytterpunkter`() {
        val idag = LocalDate.now()
        val idagStart = idag
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val idagSlutt = idag
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .minusNanos(1)

        val periode = Avstemmingsnøkkel.periode(idag)
        val avstemmingsnøkkelIdagStart = Avstemmingsnøkkel.opprett(idagStart)
        val avstemmingsnøkkelIdagSlutt = Avstemmingsnøkkel.opprett(idagSlutt)
        assertTrue(avstemmingsnøkkelIdagStart in periode)
        assertTrue(avstemmingsnøkkelIdagSlutt in periode)
    }

    @Test
    fun `avstemmingsnøkkel i 2100 er større enn dagens, og har ikke overflowet`() {
        val nå = Instant.now()
        val lengeTil = LocalDate.of(2100, 1, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        assertTrue(Avstemmingsnøkkel.opprett(lengeTil) > Avstemmingsnøkkel.opprett(nå))
    }
}
