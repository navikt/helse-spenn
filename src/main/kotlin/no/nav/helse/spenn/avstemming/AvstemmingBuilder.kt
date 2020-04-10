package no.nav.helse.spenn.avstemming

import no.nav.helse.spenn.utbetaling.OppdragDto
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.*
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.max

internal class AvstemmingBuilder(id: UUID,
                                 private val fagområde: String,
                                 private val oppdrag: List<OppdragDto>,
                                 private val detaljerPerMelding: Int = DETALJER_PER_AVSTEMMINGMELDING
) {

    private companion object {
        private const val DETALJER_PER_AVSTEMMINGMELDING = 70
        private val tidsstempel = DateTimeFormatter.ofPattern("yyyyMMddHH")
    }

    private val id = encodeUUIDBase64(id)
    private val perioder = OppdragDto.periode(oppdrag)
    private val avstemmingsnøkler =
        OppdragDto.avstemmingsperiode(oppdrag)
    private val detaljer = OppdragDto.detaljer(oppdrag)
    private val meldinger = mutableListOf(
        avstemmingsdata(AksjonType.START)
    )

    fun build(): List<Avstemmingsdata> {
        initiellDatamelding()
        detaljer.takeLast(max(0, detaljer.size - detaljerPerMelding))
            .chunked(detaljerPerMelding)
            .map { datamelding(it) }
            .forEach { meldinger.add(it) }
        meldinger.add(avstemmingsdata(AksjonType.AVSL))
        return meldinger
    }

    private fun initiellDatamelding() {
        meldinger.add(datamelding(detaljer.take(detaljerPerMelding)).apply {
            total = OppdragDto.totaldata(oppdrag)
            periode = Periodedata().apply {
                datoAvstemtFom = perioder.start.format(tidsstempel)
                datoAvstemtTom = perioder.endInclusive.format(tidsstempel)
            }
            grunnlag = OppdragDto.grunnlagsdata(oppdrag)
        })
    }

    private fun datamelding(detaljer: List<Detaljdata>) =
        avstemmingsdata(AksjonType.DATA).apply { detalj.addAll(detaljer) }

    private fun avstemmingsdata(aksjonstype: AksjonType) =
        Avstemmingsdata().apply {
            aksjon = Aksjonsdata().apply {
                aksjonType = aksjonstype
                kildeType = KildeType.AVLEV
                avstemmingType = AvstemmingType.GRSN
                avleverendeKomponentKode = "SP"
                mottakendeKomponentKode = "OS"
                underkomponentKode = fagområde
                nokkelFom = "${avstemmingsnøkler.start}"
                nokkelTom = "${avstemmingsnøkler.endInclusive}"
                avleverendeAvstemmingId = id
                brukerId = "SPENN"
            }
        }

    private fun encodeUUIDBase64(uuid: UUID): String {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
    }
}
