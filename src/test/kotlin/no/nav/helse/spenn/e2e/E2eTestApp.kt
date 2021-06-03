package no.nav.helse.spenn.e2e

import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.rapidApp

class E2eTestApp(
    var rapid: TestRapid = TestRapid(),
    val simuleringService: SimuleringService = mockk(),
    val oppdrag: TestKø = TestKø(),
    val database: TestDatabase = TestDatabase()
) {

    private fun start() {
        database.migrate()
        rapidApp(rapid, simuleringService, oppdrag, database)
        rapid.start()
    }

    private fun reset() {
        database.resetDatabase()
        rapid = TestRapid()
        clearMocks(simuleringService)
        oppdrag.reset()
    }

    companion object {
        private val testEnv by lazy{E2eTestApp()}
        fun e2eTest(f: E2eTestApp.() -> Unit) {
            try {
                testEnv.start();
                f(testEnv)
            } finally {
                testEnv.reset()
            }
        }
    }

}
