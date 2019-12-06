package no.nav.helse.spenn.oppdrag.dao

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.oppdrag.TransaksjonStatus
import no.nav.helse.spenn.oppdrag.UtbetalingsOppdrag
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*


data class TransaksjonDTO(
    val id: Long,
    val sakskompleksId: UUID,
    val utbetalingsreferanse: String,
    val nokkel: LocalDateTime? = null,
    val avstemt: Boolean = false,
    val utbetalingsOppdrag: UtbetalingsOppdrag,
    val status: TransaksjonStatus = TransaksjonStatus.STARTET,
    val oppdragResponse: String? = null
)

class OppdragStateRepository(private val dataSource: HikariDataSource) {

    private val log = LoggerFactory.getLogger(OppdragStateRepository::class.java.name)

    fun lagreOSResponse(utbetalingsreferanse: String, nøkkelAvstemming: LocalDateTime, status: TransaksjonStatus, xml: String, feilbeskrivelse: String?) {
        dataSource.connection.use {
            it.prepareStatement("update transaksjon set status = ?, oppdragresponse = ?, feilbeskrivelse = ?, modified = now() " +
                    " where oppdrag_id = (select id from oppdrag where utbetalingsreferanse = ?) and nokkel = ?").use { preparedStatement ->
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
                    where transaksjon.status = ?
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
                    where transaksjon.avstemt = false and transaksjon.nokkel < ?
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

    fun findByRefAndNokkel(utbetalingsreferanse: String, avstemmingsNøkkel: LocalDateTime) : TransaksjonDTO {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    ${DTO.selectString}
                    where oppdrag.utbetalingsreferanse = ? and transaksjon.nokkel = ?
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
        select t.id as transaksjon_id,
             sakskompleks_id, utbetalingsreferanse, nokkel, avstemt, utbetalingsoppdrag, status, oppdragresponse
        from oppdrag o join transaksjon t on oppdrag.id = transaksjon.oppdrag_id
    """.trimIndent()

        fun parse(resultSet: ResultSet): TransaksjonDTO =
            TransaksjonDTO(
                id = resultSet.getLong("transaksjon_id"),
                sakskompleksId = resultSet.getObject("sakskompleks_id", UUID::class.java),
                utbetalingsreferanse = resultSet.getString("utbetalingsreferanse"),
                nokkel = resultSet.getTimestamp("nokkel").toLocalDateTime(),
                avstemt = resultSet.getBoolean("avstemt"),
                utbetalingsOppdrag = resultSet.getString("utbetalingsoppdrag").let {
                    defaultObjectMapper.readValue(it, UtbetalingsOppdrag::class.java)
                },
                status = resultSet.getString("status").let { TransaksjonStatus.valueOf(it) },
                oppdragResponse = resultSet.getString("oppdragresponse")
            )

    }




    /*fun insert(oppdragstate: OppdragState): OppdragState

    fun delete(id: Long): OppdragState

    fun findAll(): List<OppdragState>

    fun findAllByStatus(status: OppdragStateStatus, limit: Int): List<OppdragState>

    fun findAllByAvstemtAndStatus(avstemt: Boolean, status: OppdragStateStatus): List<OppdragState>

    fun findAllNotAvstemtWithAvstemmingsnokkelNotAfter(avstemmingsnokkelMax: LocalDateTime): List<OppdragState>

    fun findById(id: Long?): OppdragState

    fun findByUtbetalingsreferanse(utbetalingsreferanse: String) : OppdragState

    fun update(oppdragstate: OppdragState): OppdragState*/

    private fun LocalDateTime?.toTimeStamp(): Timestamp? {
        return if (this != null) Timestamp.valueOf(this) else null
    }
}