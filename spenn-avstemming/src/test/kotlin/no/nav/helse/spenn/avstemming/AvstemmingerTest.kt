package no.nav.helse.spenn.avstemming

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class AvstemmingerTest {
    private val dao = mockk<OppdragDao>(relaxed = true)
    private val rapid = TestRapid().apply {
        Avstemminger(this, dao)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
    }

    @Test
    fun `oppdaterer avstemte refusjonsoppdrag`() {
        val fagområde = "SPREF"
        val nøkkelTom = 1024L
        rapid.sendTestMessage(avstemming(fagområde, nøkkelTom))
        verify(exactly = 1) {
            dao.oppdaterAvstemteOppdrag(fagområde, nøkkelTom)
        }
    }

    @Test
    fun `oppdaterer avstemte personoppdrag`() {
        val fagområde = "SP"
        val nøkkelTom = 1024L
        rapid.sendTestMessage(avstemming(fagområde, nøkkelTom))
        verify(exactly = 1) {
            dao.oppdaterAvstemteOppdrag(fagområde, nøkkelTom)
        }
    }

    @Language("JSON")
    private fun avstemming(fagområde: String, nøkkelTom: Long) = """
       {
          "@event_name": "avstemming",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "fagområde": "$fagområde",
          "detaljer": {
            "nøkkel_tom": $nøkkelTom,
            "antall_oppdrag": 1
          }
       } 
    """
}
