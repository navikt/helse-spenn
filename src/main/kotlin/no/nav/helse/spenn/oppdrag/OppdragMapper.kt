package no.nav.helse.spenn.oppdrag

import no.nav.helse.integrasjon.okonomi.oppdrag.*
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

@Component
class OppdragMapper(val objectFactory: ObjectFactory) {


    fun mapUtbetalingsOppdrag(utbetaling : UtbetalingsOppdrag): Oppdrag {

        val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
            enhet = Companion.SP_ENHET
            typeEnhet = BOS
            datoEnhetFom = toXMLDate(LocalDate.EPOCH)
        }

        val oppdrag110 = objectFactory.createOppdrag110().apply {
            kodeAksjon = utbetaling.operasjon.kode
            kodeEndring = EndringsKode.NY.kode
            kodeFagomraade = SP
            fagsystemId = utbetaling.id
            utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
            oppdragGjelderId = toFnrOrOrgnr(utbetaling.oppdragGjelder)
            saksbehId = APP
            oppdragsEnhet120.add(oppdragsEnhet)
            utbetaling.oppdragslinje.forEach {
                oppdragsLinje150.add(mapToOppdragslinje150(it))
            }
        }

        return  objectFactory.createOppdrag().apply {
            this.oppdrag110 = oppdrag110
        }


    }

    private fun mapToOppdragslinje150(oppdragslinje : OppdragsLinje) : OppdragsLinje150 {
        val grad = objectFactory.createGrad170().apply {
            typeGrad = GradTypeKode.UFØREGRAD.kode
            grad = BigInteger.valueOf(100L)
        }
        val attestant = objectFactory.createAttestant180().apply {
            attestantId = APP
        }

        return  objectFactory.createOppdragsLinje150().apply {
            kodeEndringLinje = EndringsKode.NY.kode
            kodeKlassifik = KOMPONENT_KODE
            datoVedtakFom = toXMLDate(oppdragslinje.datoFom)
            datoVedtakTom = toXMLDate(oppdragslinje.datoTom)
            delytelseId = oppdragslinje.id
            sats = oppdragslinje.sats
            fradragTillegg = TfradragTillegg.T
            typeSats = oppdragslinje.satsTypeKode.kode
            saksbehId = APP
            utbetalesTilId = toFnrOrOrgnr(oppdragslinje.utbetalesTil)
            brukKjoreplan = "N"
            grad170.add(grad)
            attestant180.add(attestant)

        }
    }

    private fun toXMLDate(dato : LocalDate) : XMLGregorianCalendar {
        return DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(GregorianCalendar.from(dato.atStartOfDay(ZoneId.systemDefault())))
    }

    private fun toFnrOrOrgnr(fonr: String) : String {
        if (fonr.length==9) return "00" + fonr
        return fonr
    }

    companion object {
        const val SP_ENHET = "4151"
        const val BOS = "BOS"
        const val KOMPONENT_KODE = "SPREFAG-IOP"
        const val APP = "SPENN"
        const val SP = "SP"
    }

}