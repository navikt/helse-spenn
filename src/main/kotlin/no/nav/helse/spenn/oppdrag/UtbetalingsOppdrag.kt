package no.nav.helse.spenn.oppdrag

import no.nav.helse.integrasjon.okonomi.oppdrag.AksjonsKode
import no.nav.helse.integrasjon.okonomi.oppdrag.SatsTypeKode
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate


data class UtbetalingsOppdrag(val id: String, //"fagsystemets identifikasjon av vedtaket"
                              val operasjon : AksjonsKode,
                              val oppdragGjelder: String, // "angir hvem som saken/vedtaket er registrert på i fagrutinen"
                              val utbetalingsLinje : List<UtbetalingsLinje>)

data class UtbetalingsLinje(val id: String, // delytelseId - "fagsystemets entydige identifikasjon av oppdragslinjen"
                            val sats: BigDecimal,
                            val satsTypeKode: SatsTypeKode,
                            val datoFom : LocalDate,
                            val datoTom : LocalDate,
                            val utbetalesTil: String,  // "kan registreres med fødselsnummer eller organisasjonsnummer til den enheten som skal motta ubetalingen. Normalt vil dette være den samme som oppdraget gjelder, men kan f.eks være en arbeidsgiver som skal få refundert pengene."
                            val grad: BigInteger)