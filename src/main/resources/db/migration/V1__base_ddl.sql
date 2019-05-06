CREATE SEQUENCE OPPDRAGSTATE_ID_SEQ;

CREATE TABLE OPPDRAGSTATE (
    ID BIGINT NOT NULL DEFAULT NEXTVAL('OPPDRAGSTATE_ID_SEQ'),
    SOKNAD_ID UUID NOT NULL,
    CREATED TIMESTAMP NOT NULL DEFAULT NOW(),
    MODIFIED TIMESTAMP NOT NULL DEFAULT NOW(),
    UTBETALINGSOPPDRAG TEXT NOT NULL,
    STATUS VARCHAR(16) NOT NULL,
    OPPDRAGRESPONSE TEXT,
    SIMULERINGRESULT TEXT,
    CONSTRAINT PK_OPPDRAGSTATE PRIMARY KEY (ID),
    CONSTRAINT UQ_SOKNAD_ID UNIQUE (SOKNAD_ID)
);

CREATE INDEX OPPDRAGSTATE_MODIFIED_IDX ON OPPDRAGSTATE(MODIFIED);
CREATE INDEX OPPDRAGSTATE_STATUS_IDX ON OPPDRAGSTATE(STATUS);
