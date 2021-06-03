package no.nav.helse.spenn.e2e

import no.nav.helse.spenn.Kø
import no.nav.helse.spenn.UtKø

class TestKø : Kø {
    val meldinger: MutableList<String> = mutableListOf()
    var listener: ((String) -> Unit)? = null

    override fun sendSession(): UtKø {
        return object : UtKø {
            override fun send(messageString: String) {
                meldinger.add(messageString)
            }

            override fun sendNoErrorHandling(messageString: String) {
                meldinger.add(messageString)
            }

        }
    }

    override fun setMessageListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    override fun start() {
    }

    override fun close() {
    }

    fun reset() {
        meldinger.clear()
        listener = null
    }

    fun meldingFraOppdrag(melding: String) {
        listener?.let { it(melding) }
    }
}