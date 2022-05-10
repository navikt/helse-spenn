ALTER TABLE oppdrag ADD COLUMN orgnr VARCHAR(32);
ALTER TABLE oppdrag DROP CONSTRAINT oppdrag_sjekksum_key;
ALTER TABLE oppdrag ADD UNIQUE (orgnr, fnr, sjekksum);

UPDATE oppdrag SET orgnr = mottaker;

ALTER TABLE oppdrag ALTER COLUMN orgnr SET NOT NULL;