package no.nav.helse.spenn.avstemming

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class TransaksjonerTest {
    private val dao = mockk<OppdragDao>(relaxed = true)
    private val rapid = TestRapid().apply {
        Transaksjoner(this, dao)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
    }

    @Test
    fun `oppdaterer oppdrag med kvittering - akseptert`() {
        val avstemmingsnøkkel = 1024L
        val fagsystemId = "asdfg"
        val fødselsnummer = "fnr"
        val alvorlighetsgrad = "00"
        val status = "AKSEPTERT"
        val kodemelding = null
        val beskrivendemelding = null
        val oppdragkvittering = "xml"

        rapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, alvorlighetsgrad, status, kodemelding, beskrivendemelding, oppdragkvittering))
        verify(exactly = 1) {
            dao.medKvittering(avstemmingsnøkkel, Oppdragstatus.AKSEPTERT, "00", kodemelding, beskrivendemelding, oppdragkvittering)
        }
    }

    @Test
    fun `oppdaterer oppdrag med kvittering - akseptert med varsel`() {
        val avstemmingsnøkkel = 1024L
        val fagsystemId = "asdfg"
        val fødselsnummer = "fnr"
        val alvorlighetsgrad = "04"
        val status = "AKSEPTERT_MED_FEIL"
        val kodemelding = "det gikk litt bra altså"
        val beskrivendemelding = null
        val oppdragkvittering = "xml"

        rapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, alvorlighetsgrad, status, kodemelding, beskrivendemelding, oppdragkvittering))
        verify(exactly = 1) {
            dao.medKvittering(avstemmingsnøkkel, Oppdragstatus.AKSEPTERT_MED_VARSEL, "04", kodemelding, beskrivendemelding, oppdragkvittering)
        }
    }

    @Test
    fun `oppdaterer oppdrag med kvittering - avvist`() {
        val avstemmingsnøkkel = 1024L
        val fagsystemId = "asdfg"
        val fødselsnummer = "fnr"
        val alvorlighetsgrad = "08"
        val status = "AVVIST"
        val kodemelding = "oppdraget finnes fra før"
        val beskrivendemelding = "huffda!"
        val oppdragkvittering = "xml"

        rapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, alvorlighetsgrad, status, kodemelding, beskrivendemelding, oppdragkvittering))
        verify(exactly = 1) {
            dao.medKvittering(avstemmingsnøkkel, Oppdragstatus.AVVIST, "08", kodemelding, beskrivendemelding, oppdragkvittering)
        }
    }

    @Test
    fun `oppdaterer oppdrag med kvittering - feil`() {
        val avstemmingsnøkkel = 1024L
        val fagsystemId = "asdfg"
        val fødselsnummer = "fnr"
        val alvorlighetsgrad = "12"
        val status = "FEIL"
        val kodemelding = "feil i oppslag mot enhetsregister"
        val beskrivendemelding = "huffda!"
        val oppdragkvittering = "xml"

        rapid.sendTestMessage(transaksjonStatus(avstemmingsnøkkel, fødselsnummer, fagsystemId, alvorlighetsgrad, status, kodemelding, beskrivendemelding, oppdragkvittering))
        verify(exactly = 1) {
            dao.medKvittering(avstemmingsnøkkel, Oppdragstatus.AVVIST, "12", kodemelding, beskrivendemelding, oppdragkvittering)
        }
    }

    @Language("JSON")
    private fun transaksjonStatus(
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        fagsystemId: String,
        alvorlighetsgrad: String,
        status: String,
        kodemelding: String?,
        beskrivendemelding: String?,
        oppdragkvittering: String
    ) = """
        {
          "@event_name": "transaksjon_status",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "fødselsnummer": "$fødselsnummer",
          "avstemmingsnøkkel": $avstemmingsnøkkel,
          "utbetalingId": "${UUID.randomUUID()}",
          "fagsystemId": "$fagsystemId",
          "feilkode_oppdrag": "$alvorlighetsgrad",
          "status": "$status",
          "kodemelding": ${kodemelding?.let { "\"$it\"" } ?: "null"},
          "beskrivendemelding": ${beskrivendemelding?.let { "\"$it\"" } ?: "null"},
          "originalXml": "$oppdragkvittering"
        }
    """
}
