package no.nav.helse.spenn

import io.mockk.mockk
import javax.jms.*

internal class TestConnection private constructor(delegate: Connection) : Connection by delegate {

    private var consumer: TestConsumer? = null
    private val meldinger = mutableListOf<Message>()
    internal val inspektør get() = ConnectionInspektør(meldinger.toList())

    constructor() : this(mockk())

    fun sendMessage(message: String) {
        consumer?.sendMessage(TestTextMessage(message))
    }

    fun reset() {
        meldinger.clear()
    }

    override fun createSession(): Session = TestSession()
    override fun createSession(transacted: Boolean, acknowledgeMode: Int) = createSession()

    private inner class TestSession private constructor(delegate: Session) : Session by delegate {
        constructor() : this(mockk())

        override fun createQueue(queueName: String) = Queue { queueName }
        override fun createTextMessage(text: String): TextMessage = TestTextMessage(text)
        override fun createProducer(destination: Destination): MessageProducer = TestProducer()
        override fun createConsumer(destination: Destination): MessageConsumer = TestConsumer().also {
            this@TestConnection.consumer = it
        }
    }

    private inner class TestProducer private constructor(delegate: MessageProducer) : MessageProducer by delegate {
        constructor() : this(mockk())

        override fun send(message: Message) {
            meldinger.add(message)
        }
    }

    private inner class TestConsumer private constructor(delegate: MessageConsumer) : MessageConsumer by delegate {
        private var listener: MessageListener? = null

        constructor() : this(mockk())

        fun sendMessage(message: Message) { listener?.onMessage(message) }

        override fun setMessageListener(listener: MessageListener) { this.listener = listener }
    }

    private class TestTextMessage private constructor(
        delegate: TextMessage,
        private var text: String
    ) : TextMessage by delegate {
        private var jmsReplyTo: Destination? = null

        constructor(text: String) : this(mockk(), text)

        override fun <T : Any?> getBody(c: Class<T>): T {
            check(c.isAssignableFrom(String::class.java))
            @Suppress("UNCHECKED_CAST")
            return text as T
        }

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
