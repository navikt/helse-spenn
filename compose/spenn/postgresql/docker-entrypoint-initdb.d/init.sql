create role "helse-spenn-oppdrag-admin";
create role "helse-spenn-oppdrag-user";
create role "helse-spenn-oppdrag-readonly";

--CREATE DATABASE IF NOT EXISTS "helse-spenn-oppdrag";

DO
$do$
BEGIN
   IF EXISTS (SELECT FROM pg_database WHERE datname = 'helse-spenn-oppdrag') THEN
      RAISE NOTICE 'Database already exists';  -- optional
   ELSE
      PERFORM dblink_exec('dbname=' || current_database()  -- current db
                        , 'CREATE DATABASE helse-spenn-oppdrag');
   END IF;
END
$do$;

GRANT "helse-spenn-oppdrag-readonly" TO "helse-spenn-oppdrag-user";
GRANT "helse-spenn-oppdrag-user" TO "helse-spenn-oppdrag-admin";

GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-admin";
GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-user";
GRANT CONNECT ON DATABASE "helse-spenn-oppdrag" TO "helse-spenn-oppdrag-readonly";


alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select, usage on sequences to "helse-spenn-oppdrag-readonly";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select on tables to "helse-spenn-oppdrag-readonly";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant select, insert, update, delete, truncate on tables to "helse-spenn-oppdrag-user";
alter default privileges for role "helse-spenn-oppdrag-admin" in schema "public" grant all privileges on tables to "helse-spenn-oppdrag-admin";



