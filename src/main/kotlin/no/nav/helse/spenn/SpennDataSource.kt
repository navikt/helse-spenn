package no.nav.helse.spenn

data class SpennDbConfig(
        val jdbcUrl: String,
        val maximumPoolSize: Int,
        val minimumIdle: Int = 1

)

class SpennDataSource private constructor(
    val config: SpennDbConfig
){

    companion object {
        private var ds : SpennDataSource? = null
        private val lock = Object()

        fun getMigratedDatasourceInstance(
                config: SpennDbConfig
        ) : SpennDataSource {
            synchronized(lock) {
                if (ds == null) {
                    val spennDataSource = SpennDataSource(config)
                    ds = spennDataSource
                }
                return ds!!
            }
        }
    }


}