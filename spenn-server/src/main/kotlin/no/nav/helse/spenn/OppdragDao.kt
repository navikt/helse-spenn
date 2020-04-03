package no.nav.helse.spenn

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDateTime
import javax.sql.DataSource

internal class OppdragDao(private val dataSource: DataSource) {

    fun oppdaterOppdrag(
        avstemmingsnøkkel: Long,
        utbetalingsreferanse: String,
        status: Oppdragstatus,
        beskrivelse: String,
        feilkode: String,
        xmlMessage: String
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET endret = now(), status = ?, beskrivelse = ?, feilkode_oppdrag = ?, oppdrag_response = ? " +
                            "WHERE avstemmingsnokkel = ? AND utbetalingsreferanse = ?",
                    status.name, beskrivelse, feilkode, xmlMessage, avstemmingsnøkkel, utbetalingsreferanse
                ).asUpdate
            )
        } == 1

    fun hentOppdragForAvstemming(avstemmingsperiode: ClosedRange<Long>) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT fnr, utbetalingsreferanse, opprettet, status, totalbelop, oppdrag_response FROM oppdrag " +
                            "WHERE ? <= avstemmingsnokkel AND avstemmingsnokkel <= ?",
                    avstemmingsperiode.start, avstemmingsperiode.endInclusive
                ).map {
                    OppdragDto(
                        fødselsnummer = it.string("fnr"),
                        utbetalingsreferanse = it.string("utbetalingsreferanse"),
                        opprettet = it.localDateTime("opprettet"),
                        status = Oppdragstatus.valueOf(it.string("status")),
                        totalbeløp = it.int("totalbelop"),
                        oppdragXml = it.stringOrNull("oppdrag_response")
                    )
                }.asList
            )
        }

    fun nyttOppdrag(
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        tidspunkt: LocalDateTime,
        utbetalingsreferanse: String,
        status: Oppdragstatus,
        totalbeløp: Int
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppdrag (avstemmingsnokkel, fnr, opprettet, utbetalingsreferanse, totalbelop, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    avstemmingsnøkkel, fødselsnummer, tidspunkt, utbetalingsreferanse, totalbeløp, status.name
                ).asUpdate
            )
        } == 1
}
