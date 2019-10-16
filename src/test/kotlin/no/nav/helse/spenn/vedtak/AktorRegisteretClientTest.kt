package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.vedtak.fnr.AktorNotFoundException
//import no.nav.helse.spenn.vedtak.fnr.AktorRegisteretClient
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import kotlin.test.assertEquals


class AktorRegisteretClientTest {

/*    val stsRestClient = Mockito.mock(StsRestClient::class.java)
    val aktorregister = Mockito.spy(AktorRegisteretClient(stsRestClient,"http://someplace"))


    @Test
    fun lookup() {
        Mockito.doReturn("token").`when`(stsRestClient).token()
        Mockito.doReturn(defaultObjectMapper.readTree(jsonAktorOK))
                .`when`(aktorregister).lookUp("1000010000000")
        Mockito.doReturn(defaultObjectMapper.readTree(jsonNotFound))
                .`when`(aktorregister).lookUp("1000020000000")
        val fnr = aktorregister.tilFnr("1000010000000")
        assertEquals("11111111111", fnr)
        assertThrows<AktorNotFoundException> {aktorregister.tilFnr("1000020000000")}
    }*/
}

val jsonAktorOK = """{"1000010000000":{"identer":[{"ident":"11111111111","identgruppe":"NorskIdent","gjeldende":true},{"ident":"1000010000000","identgruppe":"AktoerId","gjeldende":true}],"feilmelding":null}}
"""
val jsonNotFound = """{"1000020000000":{"identer":null,"feilmelding":"Den angitte personidenten finnes ikke"}}"""