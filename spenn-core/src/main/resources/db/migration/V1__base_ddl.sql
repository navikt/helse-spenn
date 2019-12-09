
create sequence oppdragstate_id_seq;

create table oppdragstate (
    id bigint not null default nextval('oppdragstate_id_seq'),
    soknad_id uuid not null,
    created timestamp not null default now(),
    modified timestamp not null default now(),
    utbetalingsoppdrag text not null,
    status varchar(16) not null,
    oppdragresponse text,
    simuleringresult text,
    constraint pk_oppdragstate primary key (id),
    constraint uq_soknad_id unique (soknad_id)
);

create index oppdragstate_modified_idx on oppdragstate(modified);
create index oppdragstate_status_idx on oppdragstate(status);

create table shedlock (
    name varchar(64),
    lock_until timestamp(3),
    locked_at timestamp(3),
    locked_by varchar(255),
    primary key (name)
);