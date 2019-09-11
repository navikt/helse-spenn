package no.nav.helse.spenn.vedtak

import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.vedtak.fnr.AktorRegisteretClient
import no.nav.helse.spenn.vedtak.fnr.StsRestClient
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito


class AktorRegisteretClientTest {

    val stsRestClient = Mockito.mock(StsRestClient::class.java)

    @Test
    fun lookup() {
        given(stsRestClient.token()).willReturn("bearer here")
        val register = AktorRegisteretClient(stsRestClient, "http://localhost")
        val json = register.lookUp("12345")
        println(defaultObjectMapper.writeValueAsString(json))

    }
}