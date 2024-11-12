package no.nav.helse.spenn

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spenn.utbetaling.OppdragDao
import no.nav.helse.spenn.utbetaling.Overføringer
import no.nav.helse.spenn.utbetaling.Transaksjoner
import no.nav.helse.spenn.utbetaling.Utbetalinger

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, dataSourceBuilder)
    rapid.start()
}

fun rapidApp(rapid: RapidsConnection, database: Database) {
    val dataSource = database.getDataSource()
    val oppdragDao = OppdragDao(dataSource)

    rapid.apply {
        Utbetalinger(this, oppdragDao)
        Overføringer(this, oppdragDao)
        Transaksjoner(this, oppdragDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                database.migrate()
            }
        })
    }
}
