package no.nav.helse.spenn

import no.nav.helse.spenn.oppdrag.AvstemmingdataXml
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.time.LocalDate
import java.util.*
import javax.jms.Connection

internal class Avstemming(
    connection: Connection,
    sendQueue: String,
    private val oppdragDao: OppdragDao
) {

    private val avstemmingdataXml = AvstemmingdataXml()
    private val jmsSession = connection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    fun avstem(id: UUID, dagen: LocalDate) {
        val avstemmingsperiode = Avstemmingsnøkkel.periode(dagen)
        val oppdrag = oppdragDao.hentOppdragForAvstemming(avstemmingsperiode.endInclusive)
        val meldinger = AvstemmingBuilder(id, oppdrag).build()
        meldinger.forEach { sendAvstemmingsmelding(it) }
        oppdragDao.oppdaterAvstemteOppdrag(avstemmingsperiode.endInclusive)
    }

    private fun sendAvstemmingsmelding(melding: Avstemmingsdata) {
        val xmlMelding = avstemmingdataXml.marshal(melding)
        producer.send(jmsSession.createTextMessage(xmlMelding))
    }
}
