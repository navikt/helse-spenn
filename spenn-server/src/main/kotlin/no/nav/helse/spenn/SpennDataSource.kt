package no.nav.helse.spenn

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spenn.config.SpennDbConfig
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

class SpennDataSource private constructor(
    val config: SpennDbConfig
){

    private val vaultPostgresUserRole = "helse-spenn-oppdrag-user"
    private val vaultPostgresAdminRole = "helse-spenn-oppdrag-admin"
    private var hikariDataSource: HikariDataSource? = null

    val dataSource: HikariDataSource
        get() = hikariDataSource!!

    companion object {
        private var ds : SpennDataSource? = null
        private val lock = Object()
        private val log = LoggerFactory.getLogger("SpennDataSource")

        fun getMigratedDatasourceInstance(
                config: SpennDbConfig
        ) : SpennDataSource {
            synchronized(lock) {
                if (ds == null) {
                    val spennDataSource = SpennDataSource(config)
                    spennDataSource.migrate()
                    spennDataSource.initUserDataSource()
                    ds = spennDataSource
                }
                return ds!!
            }
        }
    }

    private fun migrate() {
        log.info("migrating DB (vault=${config.vaultEnabled})")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            maximumPoolSize = 2
            //minimumIdle = 1
        }
        val flyDS: HikariDataSource =
                if (config.vaultEnabled)
                    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, config.vaultPostgresBackend, vaultPostgresAdminRole)
                else
                    HikariDataSource(hikariConfig)


        val flyConfig = Flyway.configure().dataSource(flyDS)
                .locations("db/migration")
                //.baselineOnMigrate(true)

        if (config.vaultEnabled) {
            flyConfig.initSql("SET ROLE \"$vaultPostgresAdminRole\"")
        }

        flyConfig.load().migrate()
        flyDS.close()
        log.info("migration complete")
    }

    private fun initUserDataSource() {
        log.info("intitiating user datasource (vault=${config.vaultEnabled})")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            maximumPoolSize = config.maximumPoolSize
            minimumIdle = config.minimumIdle
        }
        hikariDataSource =
                if (config.vaultEnabled)
                    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, config.vaultPostgresBackend, vaultPostgresUserRole)
                else
                    HikariDataSource(hikariConfig)
        log.info("ready")
    }
}