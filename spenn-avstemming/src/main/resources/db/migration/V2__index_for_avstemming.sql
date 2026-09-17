-- Begge avstemmingsspørringene filtrerer på "NOT avstemt AND status IS NOT NULL AND avstemmingsnokkel <= ?".
-- En partial index på avstemmingsnokkel, begrenset til de (få) radene som ennå ikke er avstemt, dekker begge
-- spørringene og unngår seq scan av oppdrag-tabellen.
CREATE INDEX IF NOT EXISTS idx_oppdrag_uavstemt ON oppdrag (avstemmingsnokkel) WHERE NOT avstemt AND status IS NOT NULL;
