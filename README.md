# Spenn

![Bygg og deploy](https://github.com/navikt/helse-spenn/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse

Lese utbetalingsbehov og simuleringsbehov og håndterer kommunikasjon videre mot Oppdrag/UR.

```mermaid
sequenceDiagram
    participant spleis
    participant spenn
    participant rapid
    participant spenn-avstemming   
    participant spenn-mq
    participant OS (MQ)
    
    spleis->>rapid: sender behov om utbetaling
    rapid->>spenn: lager nytt oppdrag, eller eksisterende kan sendes på nytt
    spenn->>rapid: sender MOTTATT på utbetalingsbehov
    spenn->>rapid: sender melding om at 'oppdrag til utbetaling'
    rapid->>spenn-avstemming: lager nytt oppdrag fra 'oppdrag til utbetaling'
    rapid->>spenn-mq: får melding om 'oppdrag til utbetaling'
    spenn-mq->> OS (MQ): sender oppdraget på MQ
    spenn-mq->>rapid: sender OVERFØRT på 'oppdrag til utbetaling'
    rapid->>spenn-avstemming: registrerer MANGLER_KVITTERING på oppdraget
    rapid->>spenn: videreformidler OVERFØRT på utbetalingsbehovet
    OS (MQ)->>spenn-mq: mottar kvittering fra OS
    spenn-mq->>rapid: sender transaksjon_status
    rapid->>spenn-avstemming: leser transaksjon_status og oppdaterer oppdraget i db
    rapid->>spenn: videreformidler status fra kvitteringen på utbetalingsbehovet
    rapid->>spleis: utbetalingsbehovet leses inn
    
```

### Avstemming

Avstemming kjører som en CronJob (via NaisJob) i Kubernetes:

```
% k get cronjobs -n tbd                
NAME               SCHEDULE    SUSPEND   ACTIVE   LAST SCHEDULE   AGE
spenn-avstemming   0 7 * * *   False     0        <none>          22m
```

### Simulering

Kjører som `spenn-simulering` i FSS

### Utbetaling

Kjører som `spenn` i GCP.

## Hvorfor Spenn?

Spenn heter spenn fordi det er en tjeneste som 100% bryr seg om penger. Spenn.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
