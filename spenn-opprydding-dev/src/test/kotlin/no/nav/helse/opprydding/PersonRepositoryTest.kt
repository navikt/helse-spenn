package no.nav.helse.opprydding

import org.junit.jupiter.api.Test

internal class PersonRepositoryTest: AbstractDatabaseTest() {
    @Test
    fun `slett person`() {
        opprettPerson(fødselsnummer = "EN_PERSON", avstemmingsnøkkel = 1)
        opprettPerson(fødselsnummer = "EN_ANNEN_PERSON", avstemmingsnøkkel = 2)
        assertTabellinnhold { tableSize -> tableSize == 2 }
        personRepository.slett(fødselsnummer = "EN_PERSON")
        assertTabellinnhold { tableSize -> tableSize == 1 }
    }
    @Test
    fun `slett person sletter alle rader som hører til personen`() {
        opprettPerson(fødselsnummer = "EN_PERSON", avstemmingsnøkkel = 1)
        opprettPerson(fødselsnummer = "EN_PERSON", avstemmingsnøkkel = 2)
        opprettPerson(fødselsnummer = "EN_ANNEN_PERSON", avstemmingsnøkkel = 3)
        assertTabellinnhold { tableSize -> tableSize == 3 }
        personRepository.slett(fødselsnummer = "EN_PERSON")
        assertTabellinnhold { tableSize -> tableSize == 1 }
    }
    @Test
    fun `sletting når person ikke finnes i db`() {
        opprettPerson(fødselsnummer = "EN_ANNEN_PERSON", avstemmingsnøkkel = 1)
        assertTabellinnhold { tableSize -> tableSize == 1 }
        personRepository.slett(fødselsnummer = "EN_PERSON")
        assertTabellinnhold { tableSize -> tableSize == 1 }
    }
}