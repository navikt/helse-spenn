package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.avstemmingsnokkelFormatter
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

class AvstemmingsnokkelTest {


    @Test
    fun avstemmingsnøkkelMåVære26TegnFordiUnikhetenHåndhevesAvPostgresMedMikrosekundoppløsning() {
        val dato = LocalDateTime.now()
        val formatert = dato.format(avstemmingsnokkelFormatter)
        assertEquals(26, formatert.length)
        assertEquals(dato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")), formatert)
    }
}
