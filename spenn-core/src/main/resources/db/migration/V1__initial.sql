CREATE TABLE oppdrag
(
    avstemmingsnokkel    BIGINT      NOT NULL,
    fnr                  VARCHAR(32) NOT NULL,
    opprettet            TIMESTAMP   NOT NULL,
    endret               TIMESTAMP   NULL,
    utbetalingsreferanse VARCHAR(32) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    avstemt              BOOLEAN     NOT NULL DEFAULT FALSE,
    totalbelop           INTEGER     NOT NULL,
    beskrivelse          TEXT        NULL     DEFAULT NULL,
    feilkode_oppdrag     VARCHAR(32) NULL     DEFAULT NULL,
    oppdrag_response     TEXT        NULL     DEFAULT NULL,
    CONSTRAINT pk_oppdrag PRIMARY KEY (avstemmingsnokkel)
);
CREATE INDEX oppdrag_utbetalingsref_idx ON oppdrag (utbetalingsreferanse);
CREATE INDEX oppdrag_status_idx ON oppdrag (status);

CREATE TABLE avstemming
(
    id                      UUID      NOT NULL,
    opprettet               TIMESTAMP NOT NULL DEFAULT now(),
    avstemmingsnokkel_tom   BIGINT    NOT NULL,
    antall_avstemte_oppdrag INT       NOT NULL,
    CONSTRAINT pk_avstemming PRIMARY KEY (id)
);
