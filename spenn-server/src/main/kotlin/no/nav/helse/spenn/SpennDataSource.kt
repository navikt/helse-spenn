package no.nav.helse.spenn

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

class SpennDataSource private constructor(private val env: DbEnvironment) {

    private val vaultPostgresUserRole = "helse-spenn-oppdrag-user"
    private val vaultPostgresAdminRole = "helse-spenn-oppdrag-admin"
    private var hikariDataSource: HikariDataSource? = null

    val dataSource: HikariDataSource
        get() = hikariDataSource!!

    companion object {
        private var ds: SpennDataSource? = null
        private val lock = Object()
        private val log = LoggerFactory.getLogger("SpennDataSource")

        fun getMigratedDatasourceInstance(env: DbEnvironment): SpennDataSource {
            synchronized(lock) {
                if (ds == null) {
                    val spennDataSource = SpennDataSource(env)
                    spennDataSource.migrate()
                    spennDataSource.initUserDataSource()
                    ds = spennDataSource
                }
                return ds!!
            }
        }
    }

    private fun migrate() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = env.jdbcUrl
            maximumPoolSize = 2
        }
        val flyDS: HikariDataSource = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            env.vaultPostgresMountpath,
            vaultPostgresAdminRole
        )

        val flyConfig = Flyway.configure().dataSource(flyDS).locations("db/migration")
        flyConfig.initSql("SET ROLE \"$vaultPostgresAdminRole\"")
        flyConfig.load().migrate()
        flyDS.close()
        log.info("migration complete")
    }

    private fun initUserDataSource() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = env.jdbcUrl
            maximumPoolSize = 5
            minimumIdle = 1
        }
        hikariDataSource =
            HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                hikariConfig,
                env.vaultPostgresMountpath,
                vaultPostgresUserRole
            )
        log.info("ready")
    }
}
