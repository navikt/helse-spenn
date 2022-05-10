package no.nav.helse.spenn.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spenn.utbetaling.UtbetalingerTest
import java.time.LocalDate
import java.util.*

data class Utbetalingsbehov(
    val fnr: String = "12345678911",
    val utbetalingId: UUID = UUID.randomUUID(),
    val linjer: List<Utbetalingslinje> = listOf(Utbetalingslinje()),
    val fagsystemId: String = UtbetalingerTest.FAGSYSTEMID
) {
    fun json() = jacksonObjectMapper().writeValueAsString(toMap())
    fun toMap(): Map<String, Any> {
        return mapOf(
            "@event_name" to "behov",
            "@behov" to listOf("Utbetaling"),
            "@id" to UUID.randomUUID(),
            "@behovId" to UtbetalingerTest.BEHOV,
            "organisasjonsnummer" to UtbetalingerTest.ORGNR,
            "fødselsnummer" to fnr,
            "utbetalingId" to utbetalingId.toString(),
            "Utbetaling" to mapOf(
                "mottaker" to UtbetalingerTest.ORGNR,
                "saksbehandler" to UtbetalingerTest.SAKSBEHANDLER,
                "maksdato" to "2020-04-20",
                "mottaker" to UtbetalingerTest.ORGNR,
                "fagområde" to "SPREF",
                "fagsystemId" to fagsystemId,
                "endringskode" to "NY",
                "linjer" to linjer.map { it.toMap() }
            )
        )
    }

    fun linjer(vararg linjer: Utbetalingslinje) =
        copy(linjer = linjer.toList())

    fun fnr(it: String) = copy(fnr = it)

    fun fagsystemId(it: String) = copy(fagsystemId = it)

    fun utbetalingId(it: UUID) = copy(utbetalingId = it)

    companion object {
        val utbetalingsbehov = Utbetalingsbehov()
    }
}


data class Utbetalingslinje(
    val sats: Int = 1000,
    val grad: Int? = 100,
    val fom: LocalDate = LocalDate.parse("2020-04-20"),
    val tom: LocalDate = LocalDate.parse("2020-05-20"),
    val delytelseId: Int = 1,
    val satstype: String = "DAG"
) {
    fun satstype(it: String) = copy(satstype=it)
    fun sats(it: Int) = copy(sats=it)
    fun fom(it: LocalDate) = copy(fom=it)
    fun tom(it: LocalDate) = copy(tom=it)
    fun grad(it: Int?) = copy(grad=it)

    fun toMap() =
        mapOf(
            "satstype" to satstype,
            "sats" to "$sats",
            "fom" to "$fom",
            "tom" to "$tom",
            "grad" to grad?.toString(),
            "delytelseId" to "$delytelseId",
            "refDelytelseId" to null,
            "refFagsystemId" to null,
            "endringskode" to "NY",
            "klassekode" to "SPREFAG-IOP",
            "datoStatusFom" to null,
            "statuskode" to null
        )

    companion object {
        val utbetalingslinje = Utbetalingslinje()
    }
}

data class Kvittering(
    val alvorlighetsgrad: String = AKSEPTERT_UTEN_FEIL,
    val avstemmingsnøkkel: Long = 1L,
    val fagsystemId: String = "f227ed9f-6b53-4db6-a921-bdffb8098bd3",
    val fødselsnummer: String = "12345678911",
    val saksbehandler: String = "Navn Navnesen",
    val orgnr: String = "123456789",

    ) {
    fun fagsystemId(it: String) = copy(fagsystemId=it)
    fun avstemmingsnøkkel(it: Long) = copy(avstemmingsnøkkel=it)
    fun akseptert() = copy(alvorlighetsgrad=AKSEPTERT_UTEN_FEIL)
    fun funksjonellFeil() = copy(alvorlighetsgrad=AVVIST_FUNKSJONELLE_FEIL)

    fun toXml(): String {
        return """<?xml version="1.0" encoding="utf-8"?>
    <ns2:oppdrag xmlns:ns2="http://www.trygdeetaten.no/skjema/oppdrag">
        <mmel>
            <systemId>231-OPPD</systemId>
            <alvorlighetsgrad>$alvorlighetsgrad</alvorlighetsgrad>
        </mmel>
        <oppdrag-110>
            <kodeAksjon>1</kodeAksjon>
            <kodeEndring>NY</kodeEndring>
            <kodeFagomraade>SPREF</kodeFagomraade>
            <fagsystemId>$fagsystemId</fagsystemId>
            <utbetFrekvens>MND</utbetFrekvens>
            <oppdragGjelderId>$fødselsnummer</oppdragGjelderId>
            <datoOppdragGjelderFom>1970-01-01+01:00</datoOppdragGjelderFom>
            <saksbehId>$saksbehandler</saksbehId>
            <avstemming-115>
                <kodeKomponent>SP</kodeKomponent>
                <nokkelAvstemming>$avstemmingsnøkkel</nokkelAvstemming>
                <tidspktMelding>2019-09-20-13.31.28.572227</tidspktMelding>
            </avstemming-115>
            <oppdrags-enhet-120>
                <typeEnhet>BOS</typeEnhet>
                <enhet>4151</enhet>
                <datoEnhetFom>1970-01-01+01:00</datoEnhetFom>
            </oppdrags-enhet-120>
            <oppdrags-linje-150>
                <kodeEndringLinje>NY</kodeEndringLinje>
                <delytelseId>1</delytelseId>
                <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
                <datoVedtakFom>2019-01-01+01:00</datoVedtakFom>
                <datoVedtakTom>2019-01-12+01:00</datoVedtakTom>
                <sats>600</sats>
                <fradragTillegg>T</fradragTillegg>
                <typeSats>DAG</typeSats>
                <brukKjoreplan>N</brukKjoreplan>
                <saksbehId>$saksbehandler</saksbehId>
                <refusjonsinfo-156>
                    <maksDato>2020-09-20+02:00</maksDato>
                    <refunderesId>$orgnr</refunderesId>
                    <datoFom>2019-01-01+01:00</datoFom>
                </refusjonsinfo-156>
                <grad-170>
                    <typeGrad>UFOR</typeGrad>
                    <grad>50</grad>
                </grad-170>
                <attestant-180>
                    <attestantId>$saksbehandler</attestantId>
                </attestant-180>
            </oppdrags-linje-150>
            <oppdrags-linje-150>
                <kodeEndringLinje>NY</kodeEndringLinje>
                <delytelseId>2</delytelseId>
                <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
                <datoVedtakFom>2019-02-13+01:00</datoVedtakFom>
                <datoVedtakTom>2019-02-20+01:00</datoVedtakTom>
                <sats>600</sats>
                <fradragTillegg>T</fradragTillegg>
                <typeSats>DAG</typeSats>
                <brukKjoreplan>N</brukKjoreplan>
                <saksbehId>$saksbehandler</saksbehId>
                <refusjonsinfo-156>
                    <maksDato>2020-09-20+02:00</maksDato>
                    <refunderesId>$orgnr</refunderesId>
                    <datoFom>2019-02-13+01:00</datoFom>
                </refusjonsinfo-156>
                <grad-170>
                    <typeGrad>UFOR</typeGrad>
                    <grad>70</grad>
                </grad-170>
                <attestant-180>
                    <attestantId>$saksbehandler</attestantId>
                </attestant-180>
            </oppdrags-linje-150>
            <oppdrags-linje-150>
                <kodeEndringLinje>NY</kodeEndringLinje>
                <delytelseId>3</delytelseId>
                <kodeKlassifik>SPREFAG-IOP</kodeKlassifik>
                <datoVedtakFom>2019-03-18+01:00</datoVedtakFom>
                <datoVedtakTom>2019-04-12+02:00</datoVedtakTom>
                <sats>1000</sats>
                <fradragTillegg>T</fradragTillegg>
                <typeSats>DAG</typeSats>
                <brukKjoreplan>N</brukKjoreplan>
                <saksbehId>$saksbehandler</saksbehId>
                <refusjonsinfo-156>
                    <maksDato>2020-09-20+02:00</maksDato>
                    <refunderesId>$orgnr</refunderesId>
                    <datoFom>2019-03-18+01:00</datoFom>
                </refusjonsinfo-156>
                <grad-170>
                    <typeGrad>UFOR</typeGrad>
                    <grad>100</grad>
                </grad-170>
                <attestant-180>
                    <attestantId>$saksbehandler</attestantId>
                </attestant-180>
            </oppdrags-linje-150>
        </oppdrag-110>
    </ns2:oppdrag>"""
    }



    companion object {
        val kvittering = Kvittering()
        const val AKSEPTERT_UTEN_FEIL = "00"
        const val AKSEPTERT_MED_FEIL = "04"
        const val AVVIST_FUNKSJONELLE_FEIL = "08"
        const val AVVIST_TEKNISK_FEIL = "12"
        const val UGYLDIG_FEILKODE = "??"
    }
}