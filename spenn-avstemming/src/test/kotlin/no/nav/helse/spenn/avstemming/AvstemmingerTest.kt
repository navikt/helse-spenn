package no.nav.helse.spenn.avstemming

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class AvstemmingerTest {
    private val oppdragDao = mockk<OppdragDao>(relaxed = true)
    private val avstemmingDao = mockk<AvstemmingDao>(relaxed = true)
    private val utgåendeMeldinger = mutableListOf<String>()
    private val utkø = object : UtKø {
        override fun send(messageString: String) {
            utgåendeMeldinger.add(messageString)
        }
    }
    private val rapid = TestRapid().apply {
        Avstemminger(this, oppdragDao, avstemmingDao, utkø)
    }

    @BeforeEach
    fun clear() {
        clearAllMocks()
        rapid.reset()
        utgåendeMeldinger.clear()
    }

    @Test
    fun `avstemmer ikke dersom det ikke er noe å avstemme`() {
        val dagen = LocalDate.of(2018, 1, 1)
        rapid.sendTestMessage(utførAvstemming(dagen))
        assertEquals(0, utgåendeMeldinger.size)
    }

    @Test
    fun `avstemmer oppdrag`() {
        val dagen = LocalDate.of(2018, 1, 1)
        val oppdragTilAvstemming = mapOf(
            "SPREF" to listOf(
                OppdragDto(1024L, "fnr", "fagsystemId1", LocalDateTime.now(), Oppdragstatus.AKSEPTERT, 5000, "00", null, null)
            ),
            "SP" to listOf(
                OppdragDto(1025L, "fnr", "fagsystemId2", LocalDateTime.now(), Oppdragstatus.AKSEPTERT, 5000, "00", null, null)
            )
        )
        every {
            oppdragDao.hentOppdragForAvstemming(any())
        } returns oppdragTilAvstemming

        rapid.sendTestMessage(utførAvstemming(dagen))

        verify(exactly = 1) {
            avstemmingDao.nyAvstemming(any(), "SP", any(), 1)
            avstemmingDao.nyAvstemming(any(), "SPREF", any(), 1)
            oppdragDao.oppdaterAvstemteOppdrag("SP", any())
            oppdragDao.oppdaterAvstemteOppdrag("SPREF", any())
        }
        assertEquals(6, utgåendeMeldinger.size)
    }

    @Test
    fun `oppdaterer avstemte refusjonsoppdrag`() {
        val fagområde = "SPREF"
        val nøkkelTom = 1024L
        rapid.sendTestMessage(avstemming(fagområde, nøkkelTom))
        verify(exactly = 1) {
            oppdragDao.oppdaterAvstemteOppdrag(fagområde, nøkkelTom)
        }
    }

    @Test
    fun `oppdaterer avstemte personoppdrag`() {
        val fagområde = "SP"
        val nøkkelTom = 1024L
        rapid.sendTestMessage(avstemming(fagområde, nøkkelTom))
        verify(exactly = 1) {
            oppdragDao.oppdaterAvstemteOppdrag(fagområde, nøkkelTom)
        }
    }

    @Language("JSON")
    private fun utførAvstemming(dagen: LocalDate) = """
        {
          "@event_name": "utfør_avstemming",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "dagen": "$dagen"
        }
    """

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
