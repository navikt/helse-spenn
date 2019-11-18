package no.nav.helse.spenn.blackbox

import org.apache.wss4j.common.ext.WSPasswordCallback
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler

object PasswordCallbackHandler : CallbackHandler {
    override fun handle(callbacks: Array<out Callback>) {
        callbacks
            .filterIsInstance<WSPasswordCallback>()
            .forEach { it.password = "password123" }
    }

}