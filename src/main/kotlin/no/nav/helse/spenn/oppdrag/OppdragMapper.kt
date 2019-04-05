package no.nav.helse.spenn.oppdrag

import no.nav.helse.integrasjon.okonomi.oppdrag.*
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.APP
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.BOS
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.KOMPONENT_KODE
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.SP
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.SP_ENHET
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.toFnrOrOrgnr
import no.nav.helse.integrasjon.okonomi.oppdrag.OppdragSkjemaConstants.Companion.toXMLDate
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
class OppdragMapper {

    private val objectFactory = ObjectFactory()

    fun mapUtbetalingsOppdrag(utbetaling : UtbetalingsOppdrag): Oppdrag {

        val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
            enhet = SP_ENHET
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
}