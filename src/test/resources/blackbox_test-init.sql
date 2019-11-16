create role "helse-spenn-oppdrag-admin";
create role "helse-spenn-oppdrag-user";
create role "helse-spenn-oppdrag-readonly";

GRANT "helse-spenn-oppdrag-readonly" TO "helse-spenn-oppdrag-user";
GRANT "helse-spenn-oppdrag-user" TO "helse-spenn-oppdrag-admin";

GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-admin";
GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-user";
GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-readonly";


alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select, usage on sequences to "helse-spenn-oppdrag-readonly";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select on tables to "helse-spenn-oppdrag-readonly";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select, insert, update, delete, truncate on tables to "helse-spenn-oppdrag-user";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant all privileges on tables to "helse-spenn-oppdrag-admin";



