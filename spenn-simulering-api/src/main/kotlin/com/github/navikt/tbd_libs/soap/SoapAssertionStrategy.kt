package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result
import com.github.navikt.tbd_libs.result_object.map
import com.github.navikt.tbd_libs.result_object.ok
import org.intellij.lang.annotations.Language

fun samlStrategy(
    username: String,
    password: String,
) = SoapAssertionStrategy {
    it.samlToken(username, password).map { samlToken ->
        samlToken.token.ok()
    }
}

fun usernamePasswordStrategy(
    username: String,
    password: String,
): SoapAssertionStrategy {
    @Language("XML")
    val template = """<wsse:UsernameToken>
    <wsse:Username>{{username}}</wsse:Username>
    <wsse:Password>{{password}}</wsse:Password>
</wsse:UsernameToken>"""
    return SoapAssertionStrategy {
        template
            .replace("{{username}}", username)
            .replace("{{password}}", password)
            .ok()
    }
}

fun interface SoapAssertionStrategy {
    fun token(provider: SamlTokenProvider): Result<String>
}
