ALTER TABLE oppdrag DROP CONSTRAINT oppdrag_orgnr_fnr_sjekksum_key;
ALTER TABLE oppdrag ADD UNIQUE (fnr, sjekksum);
