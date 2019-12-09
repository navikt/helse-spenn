package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.kArgThat
import no.nav.helse.spenn.kWhen
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import java.sql.Connection

internal class TransaksjonRekkefølgeTest {

    internal class BravoException : RuntimeException()

    @Test
    fun `test at det sendes order by transaksjon_id ved uthenting`() {
        // Det er viktig at oppdragene sendes i rekkefølge ved f.eks. annulering direkte etterfølgende utbetaling
        // Hvis ikke vil annulering feile, og utbetaling vil bli vellykket etterpå.

        val conn = mock(Connection::class.java)
        val hikari = mock(HikariDataSource::class.java)
        kWhen(hikari.connection).thenReturn(conn)

        kWhen(conn.prepareStatement(kArgThat {
            it.endsWith(" order by transaksjon_id")
        })).thenThrow(BravoException())

        assertThrows<BravoException> {
            OppdragStateRepository(hikari).findAllByStatus(TransaksjonStatus.SIMULERING_OK)
        }
    }

}