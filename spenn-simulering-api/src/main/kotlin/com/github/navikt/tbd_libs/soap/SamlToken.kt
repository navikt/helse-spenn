package com.github.navikt.tbd_libs.soap

import java.time.Duration
import java.time.LocalDateTime

class SamlToken(
    val token: String,
    val expirationTime: LocalDateTime,
) {
    private companion object {
        private val EXPIRATION_MARGIN = Duration.ofSeconds(10)
    }

    val isExpired get() = expirationTime <= LocalDateTime.now().plus(EXPIRATION_MARGIN)
}
