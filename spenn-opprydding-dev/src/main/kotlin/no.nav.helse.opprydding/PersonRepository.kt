package no.nav.helse.opprydding

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class PersonRepository(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {tx ->
                tx.finnAvstemmingsnøkkel(fødselsnummer).also { tx.slettAvstemming(it) }
                tx.slettOppdrag(fødselsnummer)
            }
        }
    }

    fun hentSisteOppdrag(fødselsnummer: String): List<Oppdrag> {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val stmt = """
                select fagsystem_id, min(v ->> 'fom') as fom
                from oppdrag, json_array_elements(behov->'Utbetaling'->'linjer') as v
                where fnr=?
                group by fagsystem_id;
            """.trimIndent()
            val minsteFomPerOppdrag = session.run(queryOf(stmt, fødselsnummer).map { row -> row.string("fagsystem_id") to row.localDate("fom") }.asList).toMap()

            @Language("PostgreSQL")
            val query = """
                select o1.fnr,o1.orgnr,o1.utbetaling_id,o1.fagomrade,o1.fagsystem_id,o1.mottaker,
                       cast(o1.behov->'Utbetaling'->'linjer'->-1->>'fom' as date) as nyeste_linje_fom,
                       cast(o1.behov->'Utbetaling'->'linjer'->-1->>'tom'  as date) as nyeste_linje_tom,
                       cast(o1.behov->'Utbetaling'->'linjer'->-1->>'delytelseId' as int) as nyeste_linje_delytelse_id,
                       cast(o1.behov->'Utbetaling'->'linjer'->-1->>'grad' as float) as nyeste_linje_grad,
                       cast(o1.behov->'Utbetaling'->'linjer'->-1->>'sats' as float) as nyeste_linje_sats,
                       o1.behov->'Utbetaling'->'linjer'->-1->>'klassekode' as nyeste_linje_klassekode
                from oppdrag o1
                left join oppdrag o2 on o2.fagsystem_id=o1.fagsystem_id and o2.opprettet > o1.opprettet
                where o2.fagsystem_id is null and o1.fnr=? and o1.behov->'Utbetaling'->'linjer' is not null;
            """.trimIndent()
            session.run(queryOf(query, fødselsnummer).map { row ->
                Oppdrag(
                    fnr  = row.string("fnr"),
                    orgnr = row.string("orgnr"),
                    utbetalingId = row.uuid("utbetaling_id"),
                    fagomrade = row.string("fagomrade"),
                    fagsystemId = row.string("fagsystem_id"),
                    mottaker = row.string("mottaker"),
                    eldsteFom = minsteFomPerOppdrag[row.string("fagsystem_id")],
                    fom = row.localDate("nyeste_linje_fom"),
                    tom = row.localDate("nyeste_linje_tom"),
                    grad = row.int("nyeste_linje_grad"),
                    sats = row.int("nyeste_linje_sats"),
                    delytelseId = row.int("nyeste_linje_delytelse_id"),
                    klassekode = row.string("nyeste_linje_klassekode"),
                )
            }.asList)
        }
    }

    data class Oppdrag(
        val fnr: String,
        val orgnr: String,
        val utbetalingId: UUID,
        val fagomrade: String,
        val fagsystemId: String,
        val mottaker: String,
        val eldsteFom: LocalDate?,
        val fom: LocalDate,
        val tom: LocalDate,
        val delytelseId: Int,
        val grad: Int,
        val sats: Int,
        val klassekode: String
    )

    private fun TransactionalSession.finnAvstemmingsnøkkel(fødselsnummer: String): List<Long> {
        @Language("PostgreSQL")
        val query = "SELECT avstemmingsnokkel FROM oppdrag WHERE fnr = ?"
        return run(queryOf(query, fødselsnummer).map { it.long("avstemmingsnokkel") }.asList)
    }

    private fun TransactionalSession.slettAvstemming(avstemmingsnøkler: List<Long>) {
        if (avstemmingsnøkler.isEmpty()) return
        @Language("PostgreSQL")
        val query = "DELETE FROM avstemming WHERE avstemmingsnokkel_tom in (${avstemmingsnøkler.joinToString { "?" }})"
        run(queryOf(query, *avstemmingsnøkler.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.slettOppdrag(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM oppdrag WHERE fnr = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }
}