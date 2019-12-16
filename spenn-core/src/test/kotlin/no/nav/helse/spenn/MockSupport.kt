package no.nav.helse.spenn

import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing

fun <T> any(): T = Mockito.any<T>()

fun <T> kArgThat(matcher: (T) -> Boolean): T = Mockito.argThat<T>(matcher)

fun <T> kWhen(methodCall : T) : OngoingStubbing<T> =
    Mockito.`when`(methodCall)
