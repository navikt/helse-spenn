package no.nav.helse.spenn.avstemming

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class AvstemmingDao(private val dataSource: DataSource) {

    fun nyAvstemming(
        id: UUID,
        fagområde: String,
        avstemmingsnøkkelTom: Long,
        antallOppdrag: Int
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO avstemming (id, fagomrade, avstemmingsnokkel_tom, antall_avstemte_oppdrag) " +
                            "VALUES (?, ?, ?, ?)",
                    id, fagområde, avstemmingsnøkkelTom, antallOppdrag
                ).asUpdate
            )
        } == 1
}
