package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*


internal data class TransaksjonDTO(
    val id: Long,
    val sakskompleksId: UUID,
    val utbetalingsreferanse: String,
    val nokkel: LocalDateTime? = null,
    val avstemt: Boolean = false,
    val utbetalingsOppdrag: UtbetalingsOppdrag,
    val status: TransaksjonStatus = TransaksjonStatus.STARTET,
    val oppdragResponse: String? = null
)

internal class OppdragStateRepository(private val dataSource: HikariDataSource) {

    private val log = LoggerFactory.getLogger(OppdragStateRepository::class.java.name)

    fun insertNyttOppdrag(utbetalingsOppdrag: UtbetalingsOppdrag) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("""
                    insert into oppdrag(sakskompleks_id, utbetalingsreferanse)
                    values (?, ?)
                """.trimIndent()).use { preparedStatement ->
                    preparedStatement.setObject(1, utbetalingsOppdrag.behov.sakskompleksId)
                    preparedStatement.setString(2, utbetalingsOppdrag.behov.utbetalingsreferanse)
                    preparedStatement.executeUpdate()
                }
                conn.prepareStatement("""
                    insert into transaksjon(oppdrag_id, avstemt, status, utbetalingsoppdrag)
                    values((select id from oppdrag where utbetalingsreferanse = ?), ?, ?, ?)
                """.trimIndent()).use { preparedStatement ->
                    preparedStatement.setString(1, utbetalingsOppdrag.behov.utbetalingsreferanse)
                    preparedStatement.setBoolean(2, false)
                    preparedStatement.setString(3, TransaksjonStatus.STARTET.name)
                    preparedStatement.setString(4, defaultObjectMapper.writeValueAsString(utbetalingsOppdrag))
                    preparedStatement.executeUpdate()
                }
                conn.commit()
            } catch (e: SQLException) {
                log.error("Feil i insertNyttOppdrag", e)
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun insertNyTransaksjon(utbetalingsOppdrag: UtbetalingsOppdrag) {
        dataSource.connection.use {
            it.prepareStatement("""
                insert into transaksjon(oppdrag_id, avstemt, status, utbetalingsoppdrag)
                values((select id from oppdrag where utbetalingsreferanse = ?), ?, ?, ?)
            """.trimIndent()).use { preparedStatement ->
                preparedStatement.setString(1, utbetalingsOppdrag.behov.utbetalingsreferanse)
                preparedStatement.setBoolean(2, false)
                preparedStatement.setString(3, TransaksjonStatus.STARTET.name)
                preparedStatement.setString(4, defaultObjectMapper.writeValueAsString(utbetalingsOppdrag))
                preparedStatement.executeUpdate()
            }
        }
    }

    fun lagreOSResponse(utbetalingsreferanse: String, nøkkelAvstemming: LocalDateTime, status: TransaksjonStatus, xml: String, feilbeskrivelse: String?) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update transaksjon set status = ?, oppdragresponse = ?, feilbeskrivelse = ?, modified = now()
                    where oppdrag_id = (select id from oppdrag where utbetalingsreferanse = ?) and nokkel = ?
                """).use { preparedStatement ->
                preparedStatement.setString(1, status.name)
                preparedStatement.setString(2, xml)
                preparedStatement.setString(3, feilbeskrivelse)
                preparedStatement.setString(4, utbetalingsreferanse)
                preparedStatement.setTimestamp(5, nøkkelAvstemming.toTimeStamp())
                val rowCount = preparedStatement.executeUpdate()
                log.trace("oppdaterMed: rowcount=$rowCount")
                require(rowCount == 1)
            }
        }
    }

    fun findAllByStatus(status: TransaksjonStatus, limit: Int = 100): List<TransaksjonDTO> {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    ${DTO.selectString}
                    where status = ?
                    order by transaksjon_id
                    limit ? 
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setString(1, status.name)
                preparedStatement.setInt(2, limit)
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransaksjonDTO>()
                    while (resultSet.next()) {
                        result.add(DTO.parse(resultSet))
                    }
                    return result.toList()
                }
            }
        }
    }

    fun findAllNotAvstemtWithAvstemmingsnokkelNotAfter(avstemmingsnokkelMax: LocalDateTime): List<TransaksjonDTO> {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    ${DTO.selectString}
                    where avstemt = false and nokkel < ?
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setTimestamp(1, avstemmingsnokkelMax.toTimeStamp())
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransaksjonDTO>()
                    while (resultSet.next()) {
                        result.add(DTO.parse(resultSet))
                    }
                    return result.toList()
                }
            }
        }
    }

    fun findByRef(utbetalingsreferanse: String) : List<TransaksjonDTO> {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    ${DTO.selectString}
                    where utbetalingsreferanse = ?
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setString(1, utbetalingsreferanse)
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransaksjonDTO>()
                    while (resultSet.next()) {
                        result.add(DTO.parse(resultSet))
                    }
                    return result
                }
            }
        }

    }

    fun findByRefAndNokkel(utbetalingsreferanse: String, avstemmingsNøkkel: LocalDateTime) : TransaksjonDTO {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    ${DTO.selectString}
                    where utbetalingsreferanse = ? and nokkel = ?
                """.trimIndent()
            ).use { preparedStatement ->
                preparedStatement.setString(1, utbetalingsreferanse)
                preparedStatement.setTimestamp(2, avstemmingsNøkkel.toTimeStamp())
                preparedStatement.executeQuery().use { resultSet ->
                    val result = mutableListOf<TransaksjonDTO>()
                    while (resultSet.next()) {
                        result.add(DTO.parse(resultSet))
                    }
                    require(result.size == 1) {
                        "Fikk ${result.size} resultater for $utbetalingsreferanse med nokkel $avstemmingsNøkkel"
                    }
                    return result.first()
                }
            }
        }

    }


    private object DTO {

        val selectString = """
        select transaksjon.id as transaksjon_id,
             sakskompleks_id, utbetalingsreferanse, nokkel, avstemt, utbetalingsoppdrag, status, oppdragresponse
        from oppdrag join transaksjon on oppdrag.id = transaksjon.oppdrag_id
    """.trimIndent()

        fun parse(resultSet: ResultSet): TransaksjonDTO =
            TransaksjonDTO(
                id = resultSet.getLong("transaksjon_id"),
                sakskompleksId = resultSet.getObject("sakskompleks_id", UUID::class.java),
                utbetalingsreferanse = resultSet.getString("utbetalingsreferanse"),
                nokkel = resultSet.getTimestamp("nokkel")?.toLocalDateTime(),
                avstemt = resultSet.getBoolean("avstemt"),
                utbetalingsOppdrag = resultSet.getString("utbetalingsoppdrag").let {
                    defaultObjectMapper.readValue(it, UtbetalingsOppdrag::class.java)
                },
                status = resultSet.getString("status").let { TransaksjonStatus.valueOf(it) },
                oppdragResponse = resultSet.getString("oppdragresponse")
            )

    }

    private fun LocalDateTime?.toTimeStamp(): Timestamp? {
        return if (this != null) Timestamp.valueOf(this) else null
    }
}