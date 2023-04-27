package no.nav.helse.spenn

import java.time.*
import kotlin.math.pow

internal object Avstemmingsnøkkel {
    fun opprett(tidspunkt: Instant = Instant.now()) = tidspunkt.epochSecond * 10.0.pow(9).toLong() + tidspunkt.nano
}
