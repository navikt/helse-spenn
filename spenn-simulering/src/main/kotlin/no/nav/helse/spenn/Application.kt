package no.nav.helse.spenn

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spenn.simulering.SimuleringConfig
import no.nav.helse.spenn.simulering.SimuleringService
import no.nav.helse.spenn.simulering.Simuleringer
import org.apache.cxf.bus.extension.ExtensionManagerBus
import java.io.File

fun main() {
    val env = System.getenv()
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val serviceAccountUserName = env["SERVICEUSER_NAME"] ?: "/var/run/secrets/nais.io/service_user/username".readFile()
    val serviceAccountPassword = env["SERVICEUSER_PASSWORD"] ?: "/var/run/secrets/nais.io/service_user/password".readFile()

    val simuleringConfig = SimuleringConfig(
        simuleringServiceUrl = env.getValue("SIMULERING_SERVICE_URL"),
        stsSoapUrl = env.getValue("SECURITYTOKENSERVICE_URL"),
        username = serviceAccountUserName,
        password = serviceAccountPassword,
        disableCNCheck = true
    )

    val simuleringService = SimuleringService(simuleringConfig.wrapWithSTSSimulerFpService(ExtensionManagerBus()))
    val rapid: RapidsConnection = RapidApplication.create(env)
    rapidApp(rapid, simuleringService)
    rapid.start()
}

fun rapidApp(
    rapid: RapidsConnection,
    simuleringService: SimuleringService
) {
    rapid.apply {
        Simuleringer(this, simuleringService)
    }
}

fun String.readFile() = File(this).readText(Charsets.UTF_8)
