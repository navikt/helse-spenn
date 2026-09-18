package com.github.navikt.tbd_libs.soap

import com.github.navikt.tbd_libs.result_object.Result

interface SamlTokenProvider {
    fun samlToken(
        username: String,
        password: String,
    ): Result<SamlToken>
}
