CREATE TABLE oppdrag_ny
(
    avstemmingsnokkel    BIGINT      NOT NULL,
    fnr                  VARCHAR(32) NOT NULL,
    opprettet            TIMESTAMP   NOT NULL,
    endret               TIMESTAMP   NULL,
    utbetalingsreferanse VARCHAR(32) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    beskrivelse          TEXT        NULL DEFAULT NULL,
    feilkode_oppdrag     VARCHAR(32) NULL DEFAULT NULL,
    oppdrag_response     TEXT        NULL DEFAULT NULL,
    CONSTRAINT pk_oppdrag_ny PRIMARY KEY (avstemmingsnokkel)
);
CREATE INDEX oppdrag_ny_utbetalingsref_idx ON oppdrag_ny (utbetalingsreferanse);
CREATE INDEX oppdrag_ny_status_idx ON oppdrag_ny (status);

