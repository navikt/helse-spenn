package no.nav.helse.spenn.utbetaling

import com.fasterxml.jackson.databind.ObjectMapper
import com.ibm.mq.jms.MQQueue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spenn.Utbetalingslinjer
import no.nav.helse.spenn.UtbetalingslinjerMapper
import java.time.Instant
import javax.jms.Connection
import javax.sql.DataSource

internal class FeriepengeHack(
    private val dataSource: DataSource,
    private val oppdragDao: OppdragDao,
    jmsConnection: Connection,
    sendQueue: String,
    private val replyTo: String
) {
    val mapper = ObjectMapper()
    private val jmsSession = jmsConnection.createSession()
    private val producer = jmsSession.createProducer(jmsSession.createQueue(sendQueue))

    fun trigger() {
        val oppdrag = hentOppdragMedNegativtBeløp()
        oppdrag.forEach { it ->
            it.oppdragDto.sendOppdrag(oppdragDao, it.utbetalingslinjer, Instant.now(), jmsSession, producer, MQQueue(replyTo))
            lagreFeriepengeRekjøring(it.fagsystemId, it.avstemmingnøkkel)
        }
    }

    internal fun hentOppdragMedNegativtBeløp() =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT avstemmingsnokkel, fnr, fagsystem_id, opprettet, status, totalbelop, oppdrag_response, behov FROM oppdrag o " +
                            "WHERE totalbelop < 0 AND NOT EXISTS (SELECT 1 FROM feriepenger_rekjoring fr WHERE fr.avstemmingsnokkel=o.avstemmingnokkel AND fr.fagsystemId=o.fagsystemId)",
                ).map {
                    OppdragHack(
                        oppdragDto = OppdragDto(
                            avstemmingsnøkkel = it.long("avstemmingsnokkel"),
                            fødselsnummer = it.string("fnr"),
                            fagsystemId = it.string("fagsystem_id"),
                            opprettet = it.localDateTime("opprettet"),
                            status = Oppdragstatus.valueOf(it.string("status")),
                            totalbeløp = it.int("totalbelop"),
                            oppdragXml = it.stringOrNull("oppdrag_response")
                        ),
                        utbetalingslinjer = mapUtbetalingslinjer(it.string("behov")),
                        avstemmingnøkkel = it.long("avstemmingsnokkel"),
                        fagsystemId = it.string("fagsystem_id")
                    )
                }.asList
            )
        }

    private fun mapUtbetalingslinjer(behov: String): Utbetalingslinjer {
        val behovJson = mapper.readTree(behov)
        return UtbetalingslinjerMapper(behovJson["fødselsnummer"].asText(), behovJson["organisasjonsnummer"].asText())
            .fraBehov(behovJson["Utbetaling"])
    }

    private fun lagreFeriepengeRekjøring(fagsystemId: String, avstemmingnøkkel: Long) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO feriepenger_rekjoring (fagsystemId, avstemmingsnokkel) VALUES (?, ?)",
                    fagsystemId,
                    avstemmingnøkkel
                ).asUpdate
            )
        }
    }

    data class OppdragHack(
        internal val oppdragDto: OppdragDto,
        internal val utbetalingslinjer: Utbetalingslinjer,
        internal val avstemmingnøkkel: Long,
        internal val fagsystemId: String
    )
}