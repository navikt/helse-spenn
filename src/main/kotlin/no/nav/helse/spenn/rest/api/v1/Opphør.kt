package no.nav.helse.spenn.rest.api.v1

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.routing.*
import no.nav.helse.spenn.*
import no.nav.helse.spenn.oppdrag.*
import no.nav.helse.spenn.oppdrag.dao.*
import no.nav.helse.spenn.overforing.*
import no.trygdeetaten.skjema.oppdrag.*
import org.slf4j.*
import java.time.*

private val log = LoggerFactory.getLogger("OPPHØR")

fun Route.opphør(stateService: OppdragStateService, oppdragSender: OppdragMQSender) {
    post("/cancel") {
        log.info("........cancel.......")
        val utbetalingsref = call.receive(String::class)
        val oppdragState = stateService.fetchOppdragState(utbetalingsref)
        log.info("lager opphørsmelding for ${utbetalingsref}")
        val opphørsmelding = oppdragState.lagOpphørsmelding()
        log.info("Melding: ${JAXBOppdrag().fromOppdragToXml(opphørsmelding)}")
        oppdragSender.sendOppdrag(opphørsmelding)
        log.info("Opphørsmelding Sendt...")
    }
}

private val objectFactory = ObjectFactory()
private fun OppdragStateDTO.lagOpphørsmelding(): Oppdrag {

    val dto = this

    /*val oppdragsEnhet = objectFactory.createOppdragsEnhet120().apply {
        enhet = OppdragSkjemaConstants.SP_ENHET
        typeEnhet = OppdragSkjemaConstants.BOS
        datoEnhetFom = OppdragSkjemaConstants.toXMLDate(LocalDate.EPOCH)
    }*/

    val oppdrag110 = objectFactory.createOppdrag110().apply {
        kodeAksjon = utbetalingsOppdrag.operasjon.kode

        // Opphør:
        kodeEndring = EndringsKode.ENDRING.kode
        kodeStatus = TkodeStatus.OPPH
        datoStatusFom = OppdragSkjemaConstants.toXMLDate(
                dto.utbetalingsOppdrag.utbetalingsLinje.first().datoFom
        )


        kodeFagomraade = FagOmraadekode.SYKEPENGER_REFUSJON.kode
        fagsystemId = utbetalingsOppdrag.behov.utbetalingsreferanse
        //utbetFrekvens = UtbetalingsfrekvensKode.MÅNEDLIG.kode
        oppdragGjelderId = utbetalingsOppdrag.oppdragGjelder

        //datoOppdragGjelderFom = OppdragSkjemaConstants.toXMLDate(LocalDate.EPOCH)

        datoOppdragGjelderFom = OppdragSkjemaConstants.toXMLDate(dto.created.toLocalDate())

        saksbehId = utbetalingsOppdrag.behov.saksbehandler
        avstemming115 = objectFactory.createAvstemming115().apply {
            this.nokkelAvstemming = avstemming?.nokkel?.format(avstemmingsnokkelFormatter)
            this.kodeKomponent = KomponentKode.SYKEPENGER.kode
            this.tidspktMelding = avstemming?.nokkel?.format(avstemmingsnokkelFormatter)
        }
        //oppdragsEnhet120.add(oppdragsEnhet)
        /*utbetalingsOppdrag.utbetalingsLinje.forEach {
            oppdragsLinje150.add(
                    mapTolinje150(
                            oppdragslinje = it,
                            maksDato = utbetalingsOppdrag.behov.maksdato,
                            saksbehandler = utbetalingsOppdrag.behov.saksbehandler
                    )
            )
        }*/
    }

    return objectFactory.createOppdrag().apply {
        this.oppdrag110 = oppdrag110
    }


}