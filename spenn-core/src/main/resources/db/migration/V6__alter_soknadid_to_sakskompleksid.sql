alter table OPPDRAGSTATE rename column SOKNAD_ID to SAKSKOMPLEKS_ID;

alter table OPPDRAGSTATE rename constraint UQ_SOKNAD_ID TO UQ_SAKSKOMPLEKS_ID;
