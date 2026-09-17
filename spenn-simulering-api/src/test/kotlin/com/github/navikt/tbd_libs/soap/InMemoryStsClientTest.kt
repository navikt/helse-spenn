package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.ok
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InMemoryStsClientTest {
    private companion object {
        private const val USERNAME = "foo"
        private const val PASSWORD = "bar"
    }

    @Test
    fun `henter fra kilde om cache er tom`() {
        val kilde = mockk<SamlTokenProvider>(relaxed = true)
        val token = SamlToken("<samltoken>", LocalDateTime.MAX)
        every { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) } returns token.ok()

        val cachedClient = InMemoryStsClient(kilde)
        val result = cachedClient.samlToken(USERNAME, PASSWORD)
        result as Result.Ok
        assertSame(token, result.value)
        verify(exactly = 1) { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) }
    }

    @Test
    fun `henter fra cache om verdi ikke er utgått`() {
        val kilde = mockk<SamlTokenProvider>(relaxed = true)
        val token = SamlToken("<samltoken>", LocalDateTime.MAX)
        every { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) } returns token.ok()

        val cachedClient = InMemoryStsClient(kilde)
        val result1 = cachedClient.samlToken(USERNAME, PASSWORD) // første kall
        val result2 = cachedClient.samlToken(USERNAME, PASSWORD) // andre kall
        result1 as Result.Ok
        result2 as Result.Ok
        assertSame(token, result1.value)
        assertSame(result1.value, result2.value)
        verify(exactly = 1) { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) }
    }

    @Test
    fun `henter fra kilde om verdi er utgått`() {
        val kilde = mockk<SamlTokenProvider>(relaxed = true)
        val token1 = SamlToken("<samltoken>", LocalDateTime.MIN)
        val token2 = SamlToken("<nytt samltoken>", LocalDateTime.MIN)
        every { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) } returns token1.ok() andThen token2.ok()

        val cachedClient = InMemoryStsClient(kilde)
        val result1 = cachedClient.samlToken(USERNAME, PASSWORD) // første kall
        val result2 = cachedClient.samlToken(USERNAME, PASSWORD) // andre kall, oppfrisker

        result1 as Result.Ok
        result2 as Result.Ok

        assertSame(token1, result1.value)
        assertNotSame(result1.value, result2.value)
        assertSame(result2.value, result2.value)
        verify(exactly = 2) { kilde.samlToken(eq(USERNAME), eq(PASSWORD)) }
    }
}
