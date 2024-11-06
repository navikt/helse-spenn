package no.nav.helse.spenn.avstemming

import io.mockk.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingerTest {
    private val dao = mockk<OppdragDao>(relaxed = true)
    private val rapid = TestRapid().apply {
        Utbetalinger(this, dao)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
    }

    @Test
    fun `oppretter nytt refusjonsoppdrag`() {
        val avstemmingsnøkkel = 1024L
        val utbetalingId = UUID.randomUUID()
        val fagsystemId = "asdfg"
        val fagområde = "SPREF"
        val fødselsnummer = "fnr"
        val mottaker = "mottaker"
        val totalbeløp = 5000
        val opprettet = LocalDateTime.now()

        rapid.sendTestMessage(oppdragutbetaling(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet))
        verify(exactly = 1) {
            dao.nyttOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet)
        }
    }

    @Test
    fun `oppretter nytt personoppdrag`() {
        val avstemmingsnøkkel = 1024L
        val utbetalingId = UUID.randomUUID()
        val fagsystemId = "asdfg"
        val fagområde = "SP"
        val fødselsnummer = "fnr"
        val mottaker = "mottaker"
        val totalbeløp = 5000
        val opprettet = LocalDateTime.now()

        rapid.sendTestMessage(oppdragutbetaling(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet))
        verify(exactly = 1) {
            dao.nyttOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet)
        }
    }

    @Test
    fun `ignorerer melding med kvittering`() {
        val avstemmingsnøkkel = 1024L
        val utbetalingId = UUID.randomUUID()
        val fagsystemId = "asdfg"
        val fagområde = "SP"
        val fødselsnummer = "fnr"
        val mottaker = "mottaker"
        val totalbeløp = 5000
        val opprettet = LocalDateTime.now()

        rapid.sendTestMessage(oppdragutbetalingMedKvittering(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet))
        verify(exactly = 0) {
            dao.nyttOppdrag(avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet)
        }
    }

    @Language("JSON")
    private fun oppdragutbetaling(avstemmingsnøkkel: Long, utbetalingId: UUID, fagsystemId: String, fagområde: String, fødselsnummer: String, mottaker: String, totalbeløp: Int, opprettet: LocalDateTime) = """
    {
      "@event_name": "oppdrag_utbetaling",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "$fødselsnummer",
      "utbetalingId": "$utbetalingId",
      "fagsystemId": "$fagsystemId",
      "fagområde": "$fagområde",
      "mottaker": "$mottaker",
      "opprettet": "$opprettet",
      "avstemmingsnøkkel": $avstemmingsnøkkel,
      "totalbeløp": $totalbeløp
    }
    """


    @Language("JSON")
    private fun oppdragutbetalingMedKvittering(avstemmingsnøkkel: Long, utbetalingId: UUID, fagsystemId: String, fagområde: String, fødselsnummer: String, mottaker: String, totalbeløp: Int, opprettet: LocalDateTime) = """
    {
      "@event_name": "oppdrag_utbetaling",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "$fødselsnummer",
      "utbetalingId": "$utbetalingId",
      "fagsystemId": "$fagsystemId",
      "fagområde": "$fagområde",
      "mottaker": "$mottaker",
      "opprettet": "$opprettet",
      "avstemmingsnøkkel": $avstemmingsnøkkel,
      "totalbeløp": $totalbeløp,
      "kvittering": {
        "status": "OVERFØRT"
      }
    }
    """

}
