alter table oppdragstate
    add column utbetalingsreferanse varchar(30) not null unique;

comment on column oppdragstate.utbetalingsreferanse IS 'betalingsreferanse som oppdragssystemet krever av oss.'