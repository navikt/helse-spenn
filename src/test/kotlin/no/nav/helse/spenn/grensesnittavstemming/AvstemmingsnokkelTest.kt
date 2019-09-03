package no.nav.helse.spenn.grensesnittavstemming

import no.nav.helse.spenn.avstemmingsnokkelFormatter
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals

class AvstemmingsnokkelTest {


    @Test
    fun avstemmingsnøkkelMåVære26TegnFordiUnikhetenHåndhevesAvPostgresMedMikrosekundoppløsning() {
        val dato = LocalDateTime.now()
        val formatert = dato.format(avstemmingsnokkelFormatter)
        assertEquals(26, formatert.length)
        assertEquals(dato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")), formatert)
    }

    private fun encodeUUIDBase64(uuid: UUID): String {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
    }


    @Test
    fun test1() {
        val uuid = UUID.randomUUID()
        println(uuid)
        val s = encodeUUIDBase64(uuid)
        println(s)
        val bytes = Base64.getUrlDecoder().decode(s + "==")
        val buf = ByteBuffer.wrap(bytes)
        val u = UUID(buf.long, buf.long)
        println(u)
    }
}
