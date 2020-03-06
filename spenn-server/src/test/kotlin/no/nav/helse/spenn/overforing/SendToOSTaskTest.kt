package no.nav.helse.spenn.overforing

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.dao.OppdragService
import no.nav.helse.spenn.simulering.SimuleringResult
import no.nav.helse.spenn.simulering.SimuleringStatus
import no.nav.helse.spenn.testsupport.TestDb
import no.nav.helse.spenn.testsupport.simuleringsresultat
import no.nav.helse.spenn.utbetalingMedRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private data class TransRec(val status: String, val utbetalingsreferanse: String)

class SendToOSTaskTest {

    private val dataSource = TestDb.createMigratedDataSource()
    private val service = OppdragService(dataSource)

    private val mockMQSender = mockk<OppdragMQSender>().apply {
        every { sendOppdrag(any()) } returns Unit
    }

    @Test
    fun afterSimuleringSendToOS() {
        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "1001"))
        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "1002"))
        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "1003"))
        service.hentNyeOppdrag(5).forEach {
            it.oppdaterSimuleringsresultat(simuleringsresultat)
        }

        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "1004"))
        service.hentNyeOppdrag(5).first().oppdaterSimuleringsresultat(SimuleringResult(status = SimuleringStatus.FEIL))

        service.lagreNyttOppdrag(utbetalingMedRef(utbetalingsreferanse = "1005"))

        val sendToOSTask = SendToOSTask(
            oppdragStateService = service,
            oppdragMQSender = mockMQSender
        )

        sendToOSTask.sendToOS()

        val sendtTilOS = hentSendtTilOS()
        assertEquals(3, sendtTilOS.size)
        assertEquals(setOf("1001", "1002", "1003"), sendtTilOS.map { it.utbetalingsreferanse }.toSet())
    }

    private fun hentSendtTilOS(): List<TransRec> {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select transaksjon.id as transaksjon_id, utbetalingsreferanse, status
                from oppdrag join transaksjon on oppdrag.id = transaksjon.oppdrag_id
                where status = ?
            """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setString(1, TransaksjonStatus.SENDT_OS.name)
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransRec>()
                    while (resultSet.next()) {
                        result.add(TransRec(resultSet.getString("status"), resultSet.getString("utbetalingsreferanse")))
                    }
                    return result.toList()
                }
            }
        }
    }

}
