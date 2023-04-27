package no.nav.helse.spenn.avstemming

import kotliquery.*
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class OppdragDao(private val dataSource: () -> DataSource) {
    fun hentOppdragForAvstemming(avstemmingsnøkkelTom: Long) =
        sessionOf(dataSource()).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT fagomrade, avstemmingsnokkel, fnr, fagsystem_id, utbetaling_id, opprettet, status, totalbelop, alvorlighetsgrad,kodemelding,beskrivendemelding, oppdragkvittering FROM oppdrag WHERE avstemt = FALSE AND avstemmingsnokkel <= ? AND status IS NOT NULL;"
            session.run(queryOf(query, avstemmingsnøkkelTom).map { it.string("fagomrade") to it.tilOppdragDto() }.asList)
        }.groupBy({ it.first }) { it.second }

    fun oppdaterAvstemteOppdrag(fagområde: String, avstemmingsnøkkelTom: Long) =
        sessionOf(dataSource()).use { session ->
            @Language("PostgreSQL")
            val query = "UPDATE oppdrag SET avstemt = TRUE WHERE fagomrade = CAST(? AS fagomrade) AND avstemt = FALSE AND avstemmingsnokkel <= ?"
            session.run(queryOf(query, fagområde, avstemmingsnøkkelTom).asUpdate)
        }

    fun nyttOppdrag(
        avstemmingsnøkkel: Long,
        utbetalingId: UUID,
        fagsystemId: String,
        fagområde: String,
        fødselsnummer: String,
        mottaker: String,
        totalbeløp: Int,
        opprettet: LocalDateTime
    ) = sessionOf(dataSource()).use { session ->
        @Language("PostgreSQL")
        val query =
            "INSERT INTO oppdrag (avstemmingsnokkel, utbetaling_id, fagsystem_id, fagomrade, fnr, mottaker, totalbelop, opprettet) VALUES(?, ?, ?, CAST(? AS fagomrade), ?, ?, ?, ?);"
        session.run(queryOf(query, avstemmingsnøkkel, utbetalingId, fagsystemId, fagområde, fødselsnummer, mottaker, totalbeløp, opprettet).asUpdate) == 1
    }

    fun oppdragOverført(avstemmingsnøkkel: Long) = sessionOf(dataSource()).use { session ->
        @Language("PostgreSQL")
        val query = "UPDATE oppdrag SET endret=now(),status=CAST(? AS oppdragstatus) WHERE avstemmingsnokkel = ? AND status IS NULL;"
        session.run(queryOf(query, Oppdragstatus.MANGELFULL.name, avstemmingsnøkkel).asUpdate) == 1
    }

    fun medKvittering(
        avstemmingsnøkkel: Long,
        oppdragstatus: Oppdragstatus,
        alvorlighetsgrad: String,
        kodemelding: String?,
        beskrivendemelding: String?,
        oppdragkvittering: String?
    ) = sessionOf(dataSource()).use { session ->
        @Language("PostgreSQL")
        val query =
            "UPDATE oppdrag SET endret=now(),status=CAST(? AS oppdragstatus),alvorlighetsgrad=?,kodemelding=?,beskrivendemelding=?,oppdragkvittering=? WHERE avstemt=FALSE AND avstemmingsnokkel=?;"
        session.run(queryOf(query, oppdragstatus.name, alvorlighetsgrad, kodemelding, beskrivendemelding, oppdragkvittering, avstemmingsnøkkel).asUpdate) == 1
    }

    private companion object {
        private fun Row.tilOppdragDto() =
            OppdragDto(
                avstemmingsnøkkel = long("avstemmingsnokkel"),
                fødselsnummer = string("fnr"),
                fagsystemId = string("fagsystem_id"),
                opprettet = localDateTime("opprettet"),
                status = Oppdragstatus.valueOf(string("status")),
                totalbeløp = int("totalbelop"),
                alvorlighetsgrad = stringOrNull("alvorlighetsgrad"),
                kodemelding = stringOrNull("kodemelding"),
                beskrivendemelding = stringOrNull("beskrivendemelding")
            )
    }
}
