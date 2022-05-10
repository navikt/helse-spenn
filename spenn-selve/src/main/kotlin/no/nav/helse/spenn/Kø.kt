package no.nav.helse.spenn

interface Kø {
    //TODO: Sesjonsbegrepet blir brukt likt som originalen. Vurder en sesjon per inkommende kafkamelding (i praksis en per send)
    fun sendSession(): UtKø
    fun setMessageListener(listener: (String) -> Unit)
    fun start()
    fun close()
}