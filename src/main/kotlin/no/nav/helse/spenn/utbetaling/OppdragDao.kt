package no.nav.helse.spenn.utbetaling

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
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

    fun hentBehovForOppdrag(avstemmingsnøkkel: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT behov FROM oppdrag WHERE avstemmingsnokkel = ?", avstemmingsnøkkel).map {
                JsonMessage(it.string("behov"), MessageProblems("{}"))
            }.asSingle)
        }

    fun hentOppdragForAvstemming(avstemmingsperiode: ClosedRange<Long>) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT avstemmingsnokkel, fnr, utbetalingsreferanse, opprettet, status, totalbelop, oppdrag_response FROM oppdrag " +
                            "WHERE avstemt = FALSE AND (? <= avstemmingsnokkel AND avstemmingsnokkel <= ?)",
                    avstemmingsperiode.start, avstemmingsperiode.endInclusive
                ).map { it.toOppdragDto() }.asList
            )
        }

    fun hentOppdragForAvstemming(avstemmingsnøkkelTom: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT fagomrade, avstemmingsnokkel, fnr, utbetalingsreferanse, opprettet, status, totalbelop, oppdrag_response FROM oppdrag " +
                            "WHERE avstemt = FALSE AND avstemmingsnokkel <= ?",
                    avstemmingsnøkkelTom
                ).map { it.string("fagomrade") to it.toOppdragDto() }.asList
            )
        }.groupBy({ it.first }) { it.second }

    fun oppdaterAvstemteOppdrag(fagområde: String, avstemmingsnøkkelTom: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET avstemt = TRUE WHERE fagomrade = ? AND avstemt = FALSE AND avstemmingsnokkel <= ?",
                    fagområde, avstemmingsnøkkelTom
                ).asUpdate
            )
        }

    private fun Row.toOppdragDto() =
        OppdragDto(
            avstemmingsnøkkel = long("avstemmingsnokkel"),
            fødselsnummer = string("fnr"),
            utbetalingsreferanse = string("utbetalingsreferanse"),
            opprettet = localDateTime("opprettet"),
            status = Oppdragstatus.valueOf(string("status")),
            totalbeløp = int("totalbelop"),
            oppdragXml = stringOrNull("oppdrag_response")
        )

    fun nyttOppdrag(
        fagområde: String,
        avstemmingsnøkkel: Long,
        sjekksum: Int,
        fødselsnummer: String,
        tidspunkt: LocalDateTime,
        utbetalingsreferanse: String,
        status: Oppdragstatus,
        totalbeløp: Int,
        originalJson: String
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppdrag (avstemmingsnokkel, sjekksum, fagomrade, fnr, opprettet, utbetalingsreferanse, totalbelop, status, behov) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::json) ON CONFLICT DO NOTHING",
                    avstemmingsnøkkel, sjekksum, fagområde, fødselsnummer, tidspunkt, utbetalingsreferanse, totalbeløp, status.name, originalJson
                ).asUpdate
            )
        } == 1
}
