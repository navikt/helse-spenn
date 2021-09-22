package no.nav.helse.spenn.utbetaling

import kotliquery.*
import kotliquery.action.NullableResultQueryAction
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.helse.spenn.utbetaling.Oppdragstatus.AKSEPTERT
import no.nav.helse.spenn.utbetaling.Oppdragstatus.OVERFØRT

internal class OppdragDao(private val dataSource: DataSource) {

    fun oppdaterOppdrag(
        avstemmingsnøkkel: Long,
        fagsystemId: String,
        status: Oppdragstatus,
        beskrivelse: String,
        feilkode: String,
        xmlMessage: String
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET endret = now(), status = ?, beskrivelse = ?, feilkode_oppdrag = ?, oppdrag_response = ? " +
                            "WHERE avstemmingsnokkel = ? AND fagsystem_id = ?",
                    status.name, beskrivelse, feilkode, xmlMessage, avstemmingsnøkkel, fagsystemId
                ).asUpdate
            )
        } == 1

    fun oppdaterOppdrag(
        avstemmingsnøkkel: Long,
        fagsystemId: String,
        status: Oppdragstatus
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET endret = now(), status = ? WHERE avstemmingsnokkel = ? AND fagsystem_id = ?",
                    status.name, avstemmingsnøkkel, fagsystemId
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
                    "SELECT avstemmingsnokkel, fnr, fagsystem_id, opprettet, status, totalbelop, oppdrag_response FROM oppdrag " +
                            "WHERE avstemt = FALSE AND (? <= avstemmingsnokkel AND avstemmingsnokkel <= ?)",
                    avstemmingsperiode.start, avstemmingsperiode.endInclusive
                ).map { it.toOppdragDto() }.asList
            )
        }

    fun hentOppdragForAvstemming(avstemmingsnøkkelTom: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT fagomrade, avstemmingsnokkel, fnr, fagsystem_id, opprettet, status, totalbelop, oppdrag_response FROM oppdrag " +
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



    fun finnesFraFør(fnr: String, utbetalingId: UUID): Boolean = using(sessionOf(dataSource)) { session ->
        val query =
            """SELECT 1
                FROM (select fnr, behov ->> 'utbetalingId'as utbetalingId from oppdrag) as sub
                WHERE sub.fnr = :fnr and sub.utbetalingId = :utbetalingId """
        session.run(queryOf(query, mapOf("fnr" to fnr, "utbetalingId" to utbetalingId.toString())).exists()) == true

    }

    internal fun nyttOppdrag(
        fagområde: String,
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        organisasjonsnummer: String,
        mottaker: String,
        tidspunkt: LocalDateTime,
        fagsystemId: String,
        status: Oppdragstatus,
        totalbeløp: Int,
        originalJson: String
    ): OppdragDto {
        lagre(
            fagområde,
            avstemmingsnøkkel,
            fødselsnummer,
            organisasjonsnummer,
            mottaker,
            tidspunkt,
            fagsystemId,
            status,
            totalbeløp,
            originalJson
        )
        return OppdragDto(avstemmingsnøkkel, fødselsnummer, fagsystemId, tidspunkt, status, totalbeløp, null)
    }


    private fun lagre(
        fagområde: String,
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        organisasjonsnummer: String,
        mottaker: String,
        tidspunkt: LocalDateTime,
        fagsystemId: String,
        status: Oppdragstatus,
        totalbeløp: Int,
        originalJson: String
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppdrag (avstemmingsnokkel, fagomrade, fnr, orgnr, mottaker, opprettet, fagsystem_id, totalbelop, status, behov) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json)",
                    avstemmingsnøkkel,
                    fagområde,
                    fødselsnummer,
                    organisasjonsnummer,
                    mottaker,
                    tidspunkt,
                    fagsystemId,
                    totalbeløp,
                    status.name,
                    originalJson
                ).asUpdate
            )
        } == 1

    fun hentOppdrag(fødselsnummer: String, utbetalingId: UUID): OppdragDto = using(sessionOf(dataSource)) { session ->
        val query =
            """SELECT *
                FROM (select *, behov ->> 'utbetalingId' as utbetalingId from oppdrag) as sub
                WHERE sub.fnr = :fnr and sub.utbetalingId = :utbetalingId """
        session.run(
            queryOf(
                query,
                mapOf("fnr" to fødselsnummer, "utbetalingId" to utbetalingId.toString())
            ).map { it.toOppdragDto() }.asSingle
        )!!
    }

    fun hentBorkedOppdrag(): Pair<String, OppdragDto>? = using(sessionOf(dataSource)) { session ->
        val query = """select * from oppdrag where fagsystem_id='SH7L6IWLGNGC7MLQTD625BL57Q'"""
        session.run(queryOf(query).map { it.string("behov") to it.toOppdragDto() }.asSingle)
    }


    companion object {
        fun Row.toOppdragDto() =
            OppdragDto(
                avstemmingsnøkkel = long("avstemmingsnokkel"),
                fødselsnummer = string("fnr"),
                fagsystemId = string("fagsystem_id"),
                opprettet = localDateTime("opprettet"),
                status = Oppdragstatus.valueOf(string("status")),
                totalbeløp = int("totalbelop"),
                oppdragXml = stringOrNull("oppdrag_response")
            )
    }
}

private fun Query.exists(): NullableResultQueryAction<Boolean> = this.map { true }.asSingle
