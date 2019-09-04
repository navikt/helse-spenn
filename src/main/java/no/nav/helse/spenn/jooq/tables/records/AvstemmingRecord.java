/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq.tables.records;


import java.sql.Timestamp;

import javax.annotation.processing.Generated;

import no.nav.helse.spenn.jooq.tables.Avstemming;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AvstemmingRecord extends UpdatableRecordImpl<AvstemmingRecord> implements Record4<Long, Long, Timestamp, Boolean> {

    private static final long serialVersionUID = -1518720146;

    /**
     * Setter for <code>public.avstemming.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.avstemming.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.avstemming.oppdragstate_id</code>.
     */
    public void setOppdragstateId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.avstemming.oppdragstate_id</code>.
     */
    public Long getOppdragstateId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>public.avstemming.nokkel</code>.
     */
    public void setNokkel(Timestamp value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.avstemming.nokkel</code>.
     */
    public Timestamp getNokkel() {
        return (Timestamp) get(2);
    }

    /**
     * Setter for <code>public.avstemming.avstemt</code>.
     */
    public void setAvstemt(Boolean value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.avstemming.avstemt</code>.
     */
    public Boolean getAvstemt() {
        return (Boolean) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Long, Long, Timestamp, Boolean> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Long, Long, Timestamp, Boolean> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Avstemming.AVSTEMMING.ID;
    }

    @Override
    public Field<Long> field2() {
        return Avstemming.AVSTEMMING.OPPDRAGSTATE_ID;
    }

    @Override
    public Field<Timestamp> field3() {
        return Avstemming.AVSTEMMING.NOKKEL;
    }

    @Override
    public Field<Boolean> field4() {
        return Avstemming.AVSTEMMING.AVSTEMT;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public Long component2() {
        return getOppdragstateId();
    }

    @Override
    public Timestamp component3() {
        return getNokkel();
    }

    @Override
    public Boolean component4() {
        return getAvstemt();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public Long value2() {
        return getOppdragstateId();
    }

    @Override
    public Timestamp value3() {
        return getNokkel();
    }

    @Override
    public Boolean value4() {
        return getAvstemt();
    }

    @Override
    public AvstemmingRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public AvstemmingRecord value2(Long value) {
        setOppdragstateId(value);
        return this;
    }

    @Override
    public AvstemmingRecord value3(Timestamp value) {
        setNokkel(value);
        return this;
    }

    @Override
    public AvstemmingRecord value4(Boolean value) {
        setAvstemt(value);
        return this;
    }

    @Override
    public AvstemmingRecord values(Long value1, Long value2, Timestamp value3, Boolean value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AvstemmingRecord
     */
    public AvstemmingRecord() {
        super(Avstemming.AVSTEMMING);
    }

    /**
     * Create a detached, initialised AvstemmingRecord
     */
    public AvstemmingRecord(Long id, Long oppdragstateId, Timestamp nokkel, Boolean avstemt) {
        super(Avstemming.AVSTEMMING);

        set(0, id);
        set(1, oppdragstateId);
        set(2, nokkel);
        set(3, avstemt);
    }
}
