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
            session.run(queryOf(
                "UPDATE oppdrag SET status = ?, beskrivelse = ?, feilkode_oppdrag = ?, oppdrag_response = ? " +
                        "WHERE avstemmingsnokkel = ? AND utbetalingsreferanse = ?",
                status.name, beskrivelse, feilkode, xmlMessage, avstemmingsnøkkel, utbetalingsreferanse
            ).asUpdate)
        } == 1

    fun nyttOppdrag(
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        tidspunkt: LocalDateTime,
        utbetalingsreferanse: String,
        status: Oppdragstatus
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                "INSERT INTO oppdrag (avstemmingsnokkel, fnr, opprettet, utbetalingsreferanse, status) " +
                        "VALUES (?, ?, ?, ?, ?)",
                avstemmingsnøkkel, fødselsnummer, tidspunkt, utbetalingsreferanse, status
            ).asUpdate)
        } == 1
}
