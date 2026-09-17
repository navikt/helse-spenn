package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.ok
import java.util.concurrent.ConcurrentHashMap

class InMemoryStsClient(
    private val other: SamlTokenProvider,
) : SamlTokenProvider by (other) {
    private val cache = ConcurrentHashMap<String, SamlToken>()

    override fun samlToken(
        username: String,
        password: String,
    ): Result<SamlToken> = retrieveFromCache(username) ?: storeInCache(username, password)

    private fun retrieveFromCache(username: String) = cache[username]?.takeUnless(SamlToken::isExpired)?.ok()

    private fun storeInCache(
        username: String,
        password: String,
    ): Result<SamlToken> =
        other.samlToken(username, password).also {
            if (it is Result.Ok) cache[username] = it.value
        }
}
