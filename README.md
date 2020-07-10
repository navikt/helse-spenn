# Spenn 
![Bygg og deploy](https://github.com/navikt/helse-spenn/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse
Lese utbetalingsbehov og simuleringsbehov og håndterer kommunikasjon videre mot Oppdrag/UR.

### Avstemming

Avstemming kjører som en Cronjob i Kubernetes:

```
% k get cronjobs -n tbd                
NAME    SCHEDULE    SUSPEND   ACTIVE   LAST SCHEDULE   AGE
soenn   0 7 * * *   False     0        <none>          22m
```

For å teste en cronjob (for å slippe å vente til schedule slår inn), så kan man lage en `Job` basert på cronjob:

``` 
% k create job -n tbd --from=cronjob/spenn spenn
% k get pods -n tbd
NAME          READY   STATUS     RESTARTS   AGE
spenn-8jrkm   0/1     Init:0/1   0          6s
```

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #område-helse.
