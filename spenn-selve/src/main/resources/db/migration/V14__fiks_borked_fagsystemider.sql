UPDATE oppdrag SET status = 'MOTTATT', fagsystem_id = regexp_replace(fagsystem_id, E'[\\r\\n]+', '', 'g' )
WHERE fagsystem_id LIKE E'%\r\n' AND status = 'OVERFØRT'
