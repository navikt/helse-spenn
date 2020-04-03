package no.nav.helse.spenn

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import javax.jms.Connection

internal class Avstemming(
    connection: Connection,
    sendQueue: String,
    private val oppdragDao: OppdragDao,
    private val avstemmingDao: AvstemmingDao
) {
    private companion object {
        private val log = LoggerFactory.getLogger(Avstemming::class.java)
    }
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    fun avstem(id: UUID, dagen: LocalDate) {
        val avstemmingsperiode = Avstemmingsnøkkel.periode(dagen)
        val oppdrag = oppdragDao.hentOppdragForAvstemming(avstemmingsperiode.endInclusive)
        if (oppdrag.isEmpty()) return log.info("ingenting å avstemme")
        val meldinger = AvstemmingBuilder(id, oppdrag).build()
        avstemmingDao.nyAvstemming(id, avstemmingsperiode.endInclusive, oppdrag.size)
        meldinger.forEach { sendAvstemmingsmelding(it) }
        oppdragDao.oppdaterAvstemteOppdrag(avstemmingsperiode.endInclusive)
    }

    private fun sendAvstemmingsmelding(melding: Avstemmingsdata) {
        val xmlMelding = AvstemmingdataXml.marshal(melding)
        producer.send(jmsSession.createTextMessage(xmlMelding))
    }
}
