package no.nav.helse.spenn

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class AvstemmingDao(private val dataSource: DataSource) {

    fun nyAvstemming(
        id: UUID,
        avstemmingsnøkkelTom: Long,
        antallOppdrag: Int
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO avstemming (id, avstemmingsnokkel_tom, antall_avstemte_oppdrag) " +
                            "VALUES (?, ?, ?)",
                    id, avstemmingsnøkkelTom, antallOppdrag
                ).asUpdate
            )
        } == 1
}