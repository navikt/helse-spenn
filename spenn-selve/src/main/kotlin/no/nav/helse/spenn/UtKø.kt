package no.nav.helse.spenn

interface UtKø {
    fun send(messageString: String)
    fun sendNoErrorHandling(messageString: String)
}