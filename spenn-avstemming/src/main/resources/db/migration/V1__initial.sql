CREATE TYPE oppdragstatus AS ENUM ('MANGELFULL', 'AVVIST', 'AKSEPTERT', 'AKSEPTERT_MED_VARSEL');
CREATE TYPE fagomrade AS ENUM ('SPREF', 'SP');

CREATE TABLE oppdrag
(
    avstemmingsnokkel  BIGINT PRIMARY KEY,
    utbetaling_id      UUID        NOT NULL,
    fagsystem_id       VARCHAR     NOT NULL,
    fagomrade          fagomrade   NOT NULL,
    fnr                VARCHAR(32) NOT NULL,
    mottaker           VARCHAR(32) NOT NULL,
    totalbelop         INTEGER     NOT NULL,
    opprettet          TIMESTAMP   NOT NULL,
    endret             TIMESTAMP   NULL,
    status             oppdragstatus        default null,
    avstemt            BOOLEAN     NOT NULL DEFAULT FALSE,
    alvorlighetsgrad   VARCHAR(32) NULL     DEFAULT NULL,
    kodemelding        TEXT        NULL     DEFAULT NULL,
    beskrivendemelding TEXT        NULL     DEFAULT NULL,
    oppdragkvittering  TEXT        NULL     DEFAULT NULL
);

CREATE TABLE avstemming
(
    id                      UUID        NOT NULL,
    opprettet               TIMESTAMP   NOT NULL DEFAULT now(),
    fagomrade               VARCHAR(32) NOT NULL,
    avstemmingsnokkel_tom   BIGINT      NOT NULL,
    antall_avstemte_oppdrag INT         NOT NULL,
    CONSTRAINT pk_avstemming PRIMARY KEY (id)
);
