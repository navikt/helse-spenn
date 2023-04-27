package no.nav.helse.spenn.avstemming

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    rapidApp(System.getenv())
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
        Avstemminger(this, oppdragDao)
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
