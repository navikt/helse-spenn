package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

internal class TransaksjonRekkefølgeTest {

    // TODO: Skriv om test til å sjekke order by ved å hente fra en test db.
    @Test
    fun `test at det sendes order by transaksjon_id ved uthenting`() {
        // Det er viktig at oppdragene sendes i rekkefølge ved f.eks. annulering direkte etterfølgende utbetaling
        // Hvis ikke vil annulering feile, og utbetaling vil bli vellykket etterpå.

        val connection = mockk<Connection>()
        val dataSource = mockk<HikariDataSource>()

        every { connection.close() } returns Unit
        every { connection.prepareStatement(any()) } returns mockk<PreparedStatement>().apply {
            every { setString(any(), any()) } returns Unit
            every { setInt(any(), any()) } returns Unit
            every { executeQuery() } returns mockk<ResultSet>().apply {
                every { next() } returns false
            }
        }
        every { dataSource.connection } returns connection

        TransaksjonRepository(dataSource).findAllByStatus(TransaksjonStatus.SIMULERING_OK)

        verify(exactly = 1) { connection.prepareStatement(match { it.contains(" order by transaksjon_id") }) }
    }
}
