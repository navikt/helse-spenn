alter table oppdragstate drop column avstemmingsnokkel;

create sequence avstemming_id_seq;

create table avstemming(
    id bigint not null default nextval('avstemming_id_seq'),
    oppdragstate_id bigint not null references oppdragstate(id) ON DELETE CASCADE ON UPDATE CASCADE,
    nokkel timestamp not null,
    avstemt boolean not null,
    primary key (id),
    constraint uq_avstemming_nokkel unique (nokkel),
    constraint uq_avstemming_oppdragstate unique (oppdragstate_id)
);