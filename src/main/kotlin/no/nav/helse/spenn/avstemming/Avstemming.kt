package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.Avstemmingsnøkkel
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.OppdragDto
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
        val avstemmingsperiodeForDag = Avstemmingsnøkkel.periode(dagen)
        val oppdrag = oppdragDao.hentOppdragForAvstemming(avstemmingsperiodeForDag.endInclusive)
        if (oppdrag.isEmpty()) return log.info("ingenting å avstemme")
        val avstemmingsperiode = OppdragDto.avstemmingsperiode(oppdrag)
        val meldinger = AvstemmingBuilder(id, oppdrag).build()
        avstemmingDao.nyAvstemming(id, avstemmingsperiode.endInclusive, oppdrag.size)
        log.info("avstemmer nøkkelFom=${avstemmingsperiode.start} nøkkelTom=${avstemmingsperiode.endInclusive}: sender ${meldinger.size} meldinger")
        meldinger.forEach { sendAvstemmingsmelding(it) }
        oppdragDao.oppdaterAvstemteOppdrag(avstemmingsperiode.endInclusive)
    }

    private fun sendAvstemmingsmelding(melding: Avstemmingsdata) {
        val xmlMelding = AvstemmingdataXml.marshal(melding)
        producer.send(jmsSession.createTextMessage(xmlMelding))
    }
}
