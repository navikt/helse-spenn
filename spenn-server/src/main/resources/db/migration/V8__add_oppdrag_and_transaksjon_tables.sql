create sequence oppdrag_id_seq;
create sequence transaksjon_id_seq;

create table oppdrag(
    id bigint not null default nextval('oppdrag_id_seq'),
    sakskompleks_id uuid not null,
    created timestamp not null default now(),
    utbetalingsreferanse varchar(30) not null unique,
    constraint pk_oppdrag primary key (id),
    constraint uq_oppdrag_sakskompleks_id unique (sakskompleks_id)
);

create table transaksjon(
    id bigint not null default nextval('transaksjon_id_seq'),
    oppdrag_id bigint not null references oppdrag(id) ON DELETE CASCADE ON UPDATE CASCADE,
    nokkel timestamp,
    avstemt boolean not null,
    status varchar(16) not null,
    utbetalingsoppdrag text not null,
    oppdragresponse text,
    simuleringresult text,
    feilbeskrivelse text,
    created timestamp not null default now(),
    modified timestamp not null default now(),
    primary key (id),
    constraint uq_transaksjon_nokkel unique (nokkel),
    constraint uq_transaksjon_oppdrag_id unique (oppdrag_id)
);

create index transaksjon_status_idx on transaksjon(status);

