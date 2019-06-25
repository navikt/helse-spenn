create table avstemming(
    oppdragstate_id bigint not null foreign key (oppdragstate_id) references oppdragstate(id) unique,
    nokkel timestamp not null,
    avstemt boolean not null,
    constraint uq_avstemming_nokkel unique (nokkel)
)