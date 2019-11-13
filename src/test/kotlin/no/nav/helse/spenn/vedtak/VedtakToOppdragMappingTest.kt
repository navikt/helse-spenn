package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.spenn.defaultObjectMapper
import no.nav.helse.spenn.etEnkeltBehov
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.SatsTypeKode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.test.assertEquals

class VedtakToOppdragMappingTest {

    @Test
    fun `mapping av et enkelt vedtak`() {
        val behov = etEnkeltBehov()

        val oppdragsLinje = UtbetalingsLinje(
            id = "0",
            sats = BigDecimal.valueOf(1234L),
            satsTypeKode = SatsTypeKode.DAGLIG,
            datoFom = behov.utbetalingslinjer[0].fom,
            datoTom = behov.utbetalingslinjer[0].tom,
            utbetalesTil = behov.organisasjonsnummer,
            grad = BigInteger.valueOf(100)
        )
        val målbilde = UtbetalingsOppdrag(
            behov = behov,
            operasjon = AksjonsKode.OPPDATER,
            oppdragGjelder = "12345678901",
            utbetalingsLinje = listOf(oppdragsLinje)
        )

        val faktisk = behov.tilUtbetaling("12345678901")
        Assertions.assertEquals(målbilde, faktisk)
    }

    @Test
    fun testFoersteLinjeIMappetUtbetalingslinjeSkalHaId_lik_1() {
        val node = ObjectMapper().readTree(this.javaClass.getResource("/et_utbetalingsbehov.json"))
        val behov: Utbetalingsbehov = defaultObjectMapper.treeToValue(node)
        val utbetalingsOppdrag = behov.tilUtbetaling("01010112345")
        assertEquals(1.toString(), utbetalingsOppdrag.utbetalingsLinje.first().id)
    }

    @Test
    fun testUUIDtoFagID() {
        val uuid = UUID.randomUUID();
        val fagId = uuid.toFagId()
        val decode = fagId.fromFagId()
        assertEquals(uuid, decode)
    }
}
