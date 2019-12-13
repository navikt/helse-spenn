
DO $$
    declare
        cur_oppdrag_state CURSOR FOR
            SELECT * FROM oppdragstate join avstemming on oppdragstate.id = avstemming.oppdragstate_id;
        rec_oppdrag RECORD;
    begin
        OPEN cur_oppdrag_state;
        LOOP
            fetch cur_oppdrag_state into rec_oppdrag;
            exit when not found;

            with rows as (
                insert into oppdrag (created, utbetalingsreferanse)

                    values (rec_oppdrag.created, rec_oppdrag.utbetalingsreferanse)
                    returning id
            )
            insert into transaksjon (oppdrag_id, modified, nokkel, avstemt, status, utbetalingsoppdrag, oppdragresponse, simuleringresult)
            values ((select id from rows), rec_oppdrag.modified, rec_oppdrag.nokkel, rec_oppdrag.avstemt, rec_oppdrag.status, rec_oppdrag.utbetalingsoppdrag, rec_oppdrag.oppdragresponse, rec_oppdrag.simuleringresult);
        end loop;
        close cur_oppdrag_state;
    end $$;

drop table avstemming, oppdragstate;