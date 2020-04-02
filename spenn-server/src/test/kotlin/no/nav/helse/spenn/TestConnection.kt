package no.nav.helse.spenn

import io.mockk.mockk
import javax.jms.*

internal class TestConnection private constructor(delegate: Connection) : Connection by delegate {

    private val meldinger = mutableListOf<Message>()
    internal val inspektør get() = ConnectionInspektør(meldinger.toList())

    constructor() : this(mockk())

    fun reset() {
        meldinger.clear()
    }

    override fun createSession(): Session = TestSession()

    private inner class TestSession private constructor(delegate: Session) : Session by delegate {
        constructor() : this(mockk())

        override fun createQueue(queueName: String) = Queue { queueName }
        override fun createTextMessage(text: String): TextMessage = TestTextMessage(text)
        override fun createProducer(destination: Destination): MessageProducer = TestProducer()
    }

    private inner class TestProducer private constructor(delegate: MessageProducer) : MessageProducer by delegate {
        constructor() : this(mockk())

        override fun send(message: Message) {
            meldinger.add(message)
        }
    }

    private class TestTextMessage private constructor(
        delegate: TextMessage,
        private var text: String
    ) : TextMessage by delegate {
        private var jmsReplyTo: Destination? = null

        constructor(text: String) : this(mockk(), text)

        override fun getText() = text
        override fun getJMSReplyTo() = jmsReplyTo
        override fun setJMSReplyTo(replyTo: Destination?) {
            jmsReplyTo = replyTo
        }

        override fun setText(string: String) {
            text = string
        }
    }

    class ConnectionInspektør(private val meldinger: List<Message>) {

        fun melding(indeks: Int) = meldinger[indeks]
        fun antall() = meldinger.size
    }
}
