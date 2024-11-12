package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class AppTest: AbstractDatabaseTest() {
    private lateinit var testRapid: TestRapid

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        SlettPersonRiver(testRapid, personRepository)
    }

    @Test
    fun `slettemelding medfører at person slettes fra databasen`() {
        opprettPerson("123", 1)
        assertTabellinnhold { it == 1 }
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold { it == 0 }
    }

    @Test
    fun `sletter kun aktuelt fnr`() {
        opprettPerson("123", avstemmingsnøkkel = 1)
        opprettPerson("1234", avstemmingsnøkkel = 2)
        assertTabellinnhold { it == 2 }
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold { it == 1 }
    }


    @Language("JSON")
    private fun slettemelding(fødselsnummer: String) = """
        {
          "@event_name": "slett_person",
          "@id": "${UUID.randomUUID()}",
          "opprettet": "${LocalDateTime.now()}",
          "fødselsnummer": "$fødselsnummer"
        }
    """.trimIndent()
}