package no.nav.helse.spenn.utbetaling

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotliquery.*
import kotliquery.action.NullableResultQueryAction
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class OppdragDao(private val dataSource: DataSource) {
    fun oppdaterOppdrag(
        utbetalingId: UUID,
        fagsystemId: String,
        status: Oppdragstatus,
        beskrivelse: String,
        feilkode: String,
        xmlMessage: String
    ) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET endret = now(), status = ?, beskrivelse = ?, feilkode_oppdrag = ?, oppdrag_response = ? " +
                            "WHERE utbetaling_id = ? AND fagsystem_id = ?",
                    status.name, beskrivelse, feilkode, xmlMessage, utbetalingId, fagsystemId
                ).asUpdate
            )
        } == 1

    fun oppdaterOppdrag(
        avstemmingsnøkkel: Long,
        utbetalingId: UUID,
        fagsystemId: String,
        status: Oppdragstatus
    ) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "UPDATE oppdrag SET endret = now(), avstemmingsnokkel=?, status = ? WHERE utbetaling_id = ? AND fagsystem_id = ?",
                    avstemmingsnøkkel, status.name, utbetalingId, fagsystemId
                ).asUpdate
            )
        } == 1

    fun hentBehovForOppdrag(utbetalingId: UUID, fagsystemId: String) =
        sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT behov FROM oppdrag WHERE utbetaling_id = ? AND fagsystem_id = ?", utbetalingId, fagsystemId).map {
                JsonMessage(it.string("behov"), MessageProblems("{}"), metrics)
            }.asSingle)
        }


    fun finnesFraFør(fnr: String, utbetalingId: UUID, fagsystemId: String): Boolean = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """SELECT 1 FROM oppdrag WHERE fnr = :fnr and utbetaling_id = :utbetalingId AND fagsystem_id = :fagsystemId"""
        session.run(queryOf(query, mapOf("fnr" to fnr, "utbetalingId" to utbetalingId, "fagsystemId" to fagsystemId)).exists()) == true

    }

    internal fun nyttOppdrag(
        fagområde: String,
        utbetalingId: UUID,
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
            utbetalingId,
            mottaker,
            tidspunkt,
            fagsystemId,
            status,
            totalbeløp,
            originalJson
        )
        return OppdragDto(avstemmingsnøkkel, tidspunkt, status)
    }


    private fun lagre(
        fagområde: String,
        avstemmingsnøkkel: Long,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        mottaker: String,
        tidspunkt: LocalDateTime,
        fagsystemId: String,
        status: Oppdragstatus,
        totalbeløp: Int,
        originalJson: String
    ) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppdrag (avstemmingsnokkel, fagomrade, fnr, orgnr, utbetaling_id, mottaker, opprettet, fagsystem_id, totalbelop, status, behov) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json)",
                    avstemmingsnøkkel,
                    fagområde,
                    fødselsnummer,
                    organisasjonsnummer,
                    utbetalingId,
                    mottaker,
                    tidspunkt,
                    fagsystemId,
                    totalbeløp,
                    status.name,
                    originalJson
                ).asUpdate
            )
        } == 1

    fun hentOppdrag(fødselsnummer: String, utbetalingId: UUID, fagsystemId: String): OppdragDto? = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """SELECT * FROM oppdrag WHERE fnr = :fnr and utbetaling_id = :utbetalingId AND fagsystem_id = :fagsystemId"""
        session.run(
            queryOf(
                query,
                mapOf("fnr" to fødselsnummer, "utbetalingId" to utbetalingId, "fagsystemId" to fagsystemId)
            ).map { it.toOppdragDto() }.asSingle
        )
    }

    companion object {
        private val metrics = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        fun Row.toOppdragDto() =
            OppdragDto(
                avstemmingsnøkkel = long("avstemmingsnokkel"),
                opprettet = localDateTime("opprettet"),
                status = Oppdragstatus.valueOf(string("status"))
            )
    }
}

private fun Query.exists(): NullableResultQueryAction<Boolean> = this.map { true }.asSingle
