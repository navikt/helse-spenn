package no.nav.helse.spenn.oppdrag

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.clearAllMocks
import org.apache.activemq.artemis.api.core.TransportConfiguration
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Duration
import javax.jms.Connection
import javax.jms.DeliveryMode
import javax.jms.Session

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KvitteringerIntegrationTest {
    private companion object {
        private const val PERSON = "12345678911"
        private const val ORGNR = "123456789"
        private const val UTBETALINGSREF = "f227ed9f-6b53-4db6-a921-bdffb8098bd3"
        private const val AVSTEMMINGSNØKKEL = 1L
        private const val SAKSBEHANDLER = "Navn Navnesen"
        private const val MOTTAK_QUEUE = "statusQueue"

        private const val AKSEPTERT_UTEN_FEIL = "00"
        private const val AKSEPTERT_MED_FEIL = "04"
        private const val AVVIST_FUNKSJONELLE_FEIL = "08"
        private const val AVVIST_TEKNISK_FEIL = "12"
    }

    private val server = EmbeddedActiveMQ()
        .setConfiguration(
            ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .addAcceptorConfiguration(TransportConfiguration(InVMAcceptorFactory::class.java.name))
        )

    private lateinit var connection: Connection
    private lateinit var rapid: TestRapid

    @Test
    fun `motta meldinger`() {
        connection.createSession(false, Session.AUTO_ACKNOWLEDGE).use {
            val destination = it.createQueue(MOTTAK_QUEUE)
            val producer = it.createProducer(destination)
            producer.deliveryMode = DeliveryMode.NON_PERSISTENT
            val message = it.createTextMessage(kvittering(AKSEPTERT_UTEN_FEIL))
            producer.send(message)
        }

        awaitUntilAsserted(Duration.ofSeconds(5L)) {
            assertEquals(2, rapid.inspektør.size)
            assertEquals("oppdrag_kvittering", rapid.inspektør.field(0, "@event_name").asText())
            assertEquals("transaksjon_status", rapid.inspektør.field(1, "@event_name").asText())
        }
    }

    @BeforeAll
    fun setup() {
        server.start()
        ActiveMQConnectionFactory("vm://0").also { connection = it.createConnection() }
        rapid = TestRapid().apply {
            Kvitteringer(this, Jms(connection, "sendQueue", MOTTAK_QUEUE))
        }
        connection.start()
    }

    @AfterAll
    fun teardown() {
        connection.close()
        server.stop()
    }
    @AfterEach
    fun after() {
        rapid.reset()
        clearAllMocks()
    }

    private fun awaitUntilAsserted(duration: Duration, test: () -> Unit) {
        val end = System.currentTimeMillis() + duration.toMillis()
        var error: AssertionError? = null
        while (System.currentTimeMillis() < end) {
            try {
                return test()
            } catch (err: AssertionError) {
                error = err
            }
            Thread.sleep(250)
        }
        throw error ?: fail { "failed to wait for test to pass" }
    }

    private fun kvittering(alvorlighetsgrad: String) = """<?xml version="1.0" encoding="utf-8"?>
<ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
    <mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>$alvorlighetsgrad</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>SPREF</kodeFagomraade>
        <fagsystemId>$UTBETALINGSREF</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <oppdragGjelderId>$PERSON</oppdragGjelderId>
        <datoOppdragGjelderFom>1970-01-01</datoOppdragGjelderFom>
        <saksbehId>$SAKSBEHANDLER</saksbehId>
        <avstemming-115>
            <kodeKomponent>SP</kodeKomponent>
            <nokkelAvstemming>$AVSTEMMINGSNØKKEL</nokkelAvstemming>
            <tidspktMelding>2019-09-20T13:31:28.572227</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4151</enhet>
            <datoEnhetFom>1970-01-01</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>1</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-01-01</datoVedtakFom>
            <datoVedtakTom>2019-01-12</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <henvisning>b995be7e-783d-46e0-94da-d71d6a25db42</henvisning>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-01-01</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>50</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>2</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-02-13</datoVedtakFom>
            <datoVedtakTom>2019-02-20</datoVedtakTom>
            <sats>600</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <henvisning>b995be7e-783d-46e0-94da-d71d6a25db42</henvisning>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-02-13</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>70</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>3</delytelseId>
            <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
            <datoVedtakFom>2019-03-18</datoVedtakFom>
            <datoVedtakTom>2019-04-12</datoVedtakTom>
            <sats>1000</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>DAG</typeSats>
            <henvisning>baa0b3b1-ab50-44bc-9574-a4e5c05dd2b9</henvisning>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>$SAKSBEHANDLER</saksbehId>
            <refusjonsinfo-156>
                <maksDato>2020-09-20</maksDato>
                <refunderesId>$ORGNR</refunderesId>
                <datoFom>2019-03-18</datoFom>
            </refusjonsinfo-156>
            <grad-170>
                <typeGrad>UFOR</typeGrad>
                <grad>100</grad>
            </grad-170>
            <attestant-180>
                <attestantId>$SAKSBEHANDLER</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</ns2:oppdrag>"""
}
