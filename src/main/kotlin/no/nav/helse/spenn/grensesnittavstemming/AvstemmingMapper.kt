package no.nav.helse.spenn.grensesnittavstemming

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.ObjectFactory

class AvstemmingMapper {

    companion object {
        private val objectFactory = ObjectFactory()
    }

    internal fun lagAvstemmingsdataFelles(/*aksjonType: AksjonType*/): Avstemmingsdata =
            objectFactory.createAvstemmingsdata().apply {
                //aksjon = tilAksjonsdata(aksjonType)
            }

}